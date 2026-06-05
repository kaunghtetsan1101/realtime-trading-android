package com.tradingapp.watchlist

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.usecase.GetWatchlistUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.SyncAssetsUseCase
import com.tradingapp.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val getWatchlist = mockk<GetWatchlistUseCase>()
    private val syncAssets = mockk<SyncAssetsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val observeNetworkStatus = mockk<ObserveNetworkStatusUseCase>()

    private lateinit var viewModel: WatchlistViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getWatchlist() } returns flowOf(Result.Loading)
        coEvery { syncAssets() } returns kotlin.Result.success(Unit)
        every { observeNetworkStatus() } returns flowOf(true) // online by default
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = buildViewModel()
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `state updates to success when watchlist emits data`() = runTest {
        val assets = listOf(fakeAsset("BTC"), fakeAsset("ETH"))
        every { getWatchlist() } returns flowOf(Result.Loading, Result.Success(assets))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(assets, state.assets)
        assertNull(state.error)
    }

    @Test
    fun `state updates to error when watchlist emits error`() = runTest {
        val exception = RuntimeException("Network error")
        every { getWatchlist() } returns flowOf(Result.Error(exception))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error) // RuntimeException.localizedMessage passthrough via ErrorMapper
    }

    @Test
    fun `AssetClicked event sends NavigateToDetail effect`() = runTest {
        viewModel = buildViewModel()
        viewModel.effects.test {
            viewModel.onEvent(WatchlistEvent.AssetClicked("SOL"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(WatchlistEffect.NavigateToDetail("SOL"), awaitItem())
        }
    }

    @Test
    fun `ToggleFavorite calls use case with correct args`() = runTest {
        coEvery { toggleFavorite("BTC", true) } returns Unit
        viewModel = buildViewModel()

        viewModel.onEvent(WatchlistEvent.ToggleFavorite("BTC", true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavorite("BTC", true) }
    }

    @Test
    fun `sync failure shows snackbar effect without clearing assets`() = runTest {
        val assets = listOf(fakeAsset("BTC"))
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        coEvery { syncAssets() } returns kotlin.Result.failure(RuntimeException("Timeout"))

        viewModel = buildViewModel()
        viewModel.effects.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is WatchlistEffect.ShowSnackbar)
        }

        assertEquals(assets, viewModel.state.value.assets)
    }

    @Test
    fun `network offline sets isOffline true`() = runTest {
        val networkFlow = MutableStateFlow(true)
        every { observeNetworkStatus() } returns networkFlow

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isOffline)

        networkFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isOffline)
    }

    @Test
    fun `network back online clears isOffline`() = runTest {
        val networkFlow = MutableStateFlow(false)
        every { observeNetworkStatus() } returns networkFlow

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isOffline)

        networkFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isOffline)
    }

    // --- Helpers ---

    private fun buildViewModel() = WatchlistViewModel(getWatchlist, toggleFavorite, syncAssets, observeNetworkStatus)

    private fun fakeAsset(symbol: String) = Asset(
        symbol = symbol,
        name = symbol,
        currentPrice = 100.0,
        priceChange24h = 1.0,
        priceChangePct24h = 1.0,
        marketCap = 1_000_000.0,
        volume24h = 100_000.0,
        logoUrl = null,
        isFavorite = false,
        lastUpdated = 0L,
    )
}
