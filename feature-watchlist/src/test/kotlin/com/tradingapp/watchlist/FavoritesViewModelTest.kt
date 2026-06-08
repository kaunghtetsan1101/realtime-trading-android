package com.tradingapp.watchlist

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.usecase.GetFavoritesUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
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
class FavoritesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val getFavorites = mockk<GetFavoritesUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val observeNetworkStatus = mockk<ObserveNetworkStatusUseCase>()

    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getFavorites() } returns flowOf(Result.Loading)
        every { observeNetworkStatus() } returns flowOf(true)
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
    fun `state shows favourited assets on success`() = runTest {
        val favorites = listOf(fakeAsset("BTC"), fakeAsset("ETH"))
        every { getFavorites() } returns flowOf(Result.Loading, Result.Success(favorites))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(favorites, state.assets)
        assertNull(state.error)
    }

    @Test
    fun `empty list produces no-error success state`() = runTest {
        every { getFavorites() } returns flowOf(Result.Success(emptyList()))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.assets.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `AssetClicked event sends NavigateToDetail effect`() = runTest {
        viewModel = buildViewModel()
        viewModel.effects.test {
            viewModel.onEvent(WatchlistEvent.AssetClicked("BTC"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(WatchlistEffect.NavigateToDetail("BTC"), awaitItem())
        }
    }

    @Test
    fun `ToggleFavorite calls use case with correct args`() = runTest {
        coEvery { toggleFavorite("BTC", false) } returns Unit
        viewModel = buildViewModel()

        viewModel.onEvent(WatchlistEvent.ToggleFavorite("BTC", false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavorite("BTC", false) }
    }

    @Test
    fun `Refresh event is a no-op — no sync triggered`() = runTest {
        every { getFavorites() } returns flowOf(Result.Success(listOf(fakeAsset("BTC"))))
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Sending Refresh should not crash or clear assets
        viewModel.onEvent(WatchlistEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.assets.size)
    }

    @Test
    fun `offline flag updates with network status`() = runTest {
        val networkFlow = MutableStateFlow(true)
        every { observeNetworkStatus() } returns networkFlow

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isOffline)

        networkFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isOffline)
    }

    // --- Helpers ---

    private fun buildViewModel() = FavoritesViewModel(getFavorites, toggleFavorite, observeNetworkStatus)

    private fun fakeAsset(symbol: String) = Asset(
        symbol = symbol,
        name = symbol,
        currentPrice = 100.0,
        priceChange24h = 1.0,
        priceChangePct24h = 1.0,
        marketCap = 1_000_000.0,
        volume24h = 100_000.0,
        logoUrl = null,
        isFavorite = true,
        lastUpdated = 0L,
    )
}
