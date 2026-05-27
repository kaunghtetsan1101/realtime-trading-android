package com.tradingapp.marketdetail

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val getAssetDetail = mockk<GetAssetDetailUseCase>()
    private val observePriceTicks = mockk<ObservePriceTicksUseCase>()
    private val observeNetworkStatus = mockk<ObserveNetworkStatusUseCase>()

    private val ticksFlow = MutableSharedFlow<PriceTick>(replay = 0, extraBufferCapacity = 16)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observePriceTicks(SYMBOL) } returns ticksFlow
        every { observeNetworkStatus() } returns flowOf(true) // online by default
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Loading)

        val vm = buildViewModel()

        assertTrue(vm.state.value.isLoading)
        assertNull(vm.state.value.asset)
    }

    @Test
    fun `success emission produces content state`() = runTest {
        val asset = fakeAsset()
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Loading, Result.Success(asset))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.asset)
        assertEquals(asset, state.asset)
        assertNull(state.error)
    }

    @Test
    fun `error emission produces error state`() = runTest {
        val exception = RuntimeException("Network error")
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Error(exception))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `price tick updates currentPrice on asset`() = runTest {
        val asset = fakeAsset()
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Success(asset))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tick = PriceTick(SYMBOL, 70_000.0, System.currentTimeMillis())
        ticksFlow.emit(tick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            70_000.0,
            vm.state.value.asset
                ?.currentPrice,
        )
    }

    @Test
    fun `price tick appends to recentPrices`() = runTest {
        val asset = fakeAsset()
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Success(asset))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tick = PriceTick(SYMBOL, 69_000.0, System.currentTimeMillis())
        ticksFlow.emit(tick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            vm.state.value.recentPrices
                .isNotEmpty(),
        )
        assertEquals(
            69_000.0,
            vm.state.value.recentPrices
                .last(),
            0.0,
        )
    }

    @Test
    fun `recentPrices is capped at MAX_PRICE_HISTORY (50)`() = runTest {
        val asset = fakeAsset()
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Success(asset))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        repeat(60) { i ->
            ticksFlow.emit(PriceTick(SYMBOL, i.toDouble(), System.currentTimeMillis()))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertTrue(vm.state.value.recentPrices.size <= 50)
    }

    @Test
    fun `NavigateBack event sends NavigateBack effect`() = runTest {
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Loading)

        val vm = buildViewModel()
        vm.effects.test {
            vm.onEvent(MarketDetailEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(MarketDetailEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `Retry event reloads asset data`() = runTest {
        val asset = fakeAsset()
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Error(RuntimeException("fail")))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.state.value.error)

        // Now make the use case succeed on next call
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Success(asset))
        vm.onEvent(MarketDetailEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertNotNull(vm.state.value.asset)
    }

    @Test
    fun `network offline sets isOffline true`() = runTest {
        val networkFlow = MutableStateFlow(true)
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Loading)
        every { observeNetworkStatus() } returns networkFlow

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isOffline)

        networkFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.isOffline)
    }

    @Test
    fun `network back online clears isOffline`() = runTest {
        val networkFlow = MutableStateFlow(false)
        every { getAssetDetail(SYMBOL) } returns flowOf(Result.Loading)
        every { observeNetworkStatus() } returns networkFlow

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.isOffline)

        networkFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isOffline)
    }

    // --- Helpers ---

    private fun buildViewModel() = MarketDetailViewModel(
        symbol = SYMBOL,
        getAssetDetail = getAssetDetail,
        observePriceTicks = observePriceTicks,
        observeNetworkStatus = observeNetworkStatus,
    )

    private fun fakeAsset() = Asset(
        symbol = SYMBOL,
        name = "Bitcoin",
        currentPrice = 67_500.0,
        priceChange24h = 1_575.0,
        priceChangePct24h = 2.34,
        high24h = 68_200.0,
        low24h = 66_100.0,
        marketCap = 1_320_000_000_000.0,
        volume24h = 28_500_000_000.0,
        logoUrl = null,
        isFavorite = false,
        lastUpdated = 0L,
    )

    companion object {
        private const val SYMBOL = "BTC"
    }
}
