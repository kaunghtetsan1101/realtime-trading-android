package com.tradingapp.search

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.usecase.GetWatchlistUseCase
import com.tradingapp.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val getWatchlist = mockk<GetWatchlistUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getWatchlist() } returns flowOf(Result.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- State tests ---

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = buildViewModel()
        // Combine hasn't fired yet (debounce needs 300ms); initial state holds.
        assertTrue(viewModel.state.value.isLoading)
        assertTrue(
            viewModel.state.value.results
                .isEmpty(),
        )
    }

    @Test
    fun `empty query shows all assets`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        advanceTimeBy(350) // past the 300ms debounce on the initial empty query
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(assets, viewModel.state.value.results)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `query filters by symbol`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.QueryChanged("BTC"))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.results.size)
        assertEquals(
            "BTC",
            viewModel.state.value.results
                .first()
                .symbol,
        )
    }

    @Test
    fun `query filters by name`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.QueryChanged("Bitcoin"))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.results.size)
        assertEquals(
            "BTC",
            viewModel.state.value.results
                .first()
                .symbol,
        )
    }

    @Test
    fun `filter FAVORITES shows only favorited assets`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0, fav = true),
                fakeAsset("ETH", "Ethereum", pct = -1.0, fav = false),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.FilterSelected(AssetFilter.FAVORITES))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.results.size)
        assertTrue(
            viewModel.state.value.results
                .first()
                .isFavorite,
        )
    }

    @Test
    fun `filter GAINERS shows only positive pct change`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
                fakeAsset("SOL", "Solana", pct = 5.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.FilterSelected(AssetFilter.GAINERS))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.state.value.results
        assertEquals(2, results.size)
        assertTrue(results.all { it.priceChangePct24h > 0 })
    }

    @Test
    fun `filter LOSERS shows only negative pct change`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
                fakeAsset("SOL", "Solana", pct = -3.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.FilterSelected(AssetFilter.LOSERS))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.state.value.results
        assertEquals(2, results.size)
        assertTrue(results.all { it.priceChangePct24h < 0 })
    }

    @Test
    fun `sort PRICE_CHANGE_DESC orders highest first`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("SOL", "Solana", pct = 5.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.SortSelected(SortOrder.PRICE_CHANGE_DESC))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.state.value.results
        assertEquals(3, results.size)
        assertEquals("SOL", results[0].symbol)
        assertEquals("BTC", results[1].symbol)
        assertEquals("ETH", results[2].symbol)
    }

    @Test
    fun `sort VOLUME_DESC orders highest volume first`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", vol = 1_000_000.0),
                fakeAsset("ETH", "Ethereum", vol = 5_000_000.0),
                fakeAsset("SOL", "Solana", vol = 500_000.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.SortSelected(SortOrder.VOLUME_DESC))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.state.value.results
        assertEquals(3, results.size)
        assertEquals("ETH", results[0].symbol)
        assertEquals("BTC", results[1].symbol)
        assertEquals("SOL", results[2].symbol)
    }

    @Test
    fun `no matching query returns empty results`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.QueryChanged("ZZZNOMATCH"))
        advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            viewModel.state.value.results
                .isEmpty(),
        )
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `rapid query changes debounce — filter fires once`() = runTest {
        val assets =
            listOf(
                fakeAsset("BTC", "Bitcoin", pct = 2.0),
                fakeAsset("ETH", "Ethereum", pct = -1.0),
            )
        every { getWatchlist() } returns flowOf(Result.Success(assets))
        viewModel = buildViewModel()

        // Rapid successive changes — only the last value should drive the combine.
        viewModel.onEvent(SearchEvent.QueryChanged("B"))
        viewModel.onEvent(SearchEvent.QueryChanged("BT"))
        viewModel.onEvent(SearchEvent.QueryChanged("BTC"))

        // At 250ms the 300ms debounce hasn't elapsed — results unchanged (empty initial state).
        advanceTimeBy(250)
        assertNotEquals(1, viewModel.state.value.results.size)

        // At 300ms more (550ms total) the debounce fires with "BTC".
        advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.results.size)
        assertEquals(
            "BTC",
            viewModel.state.value.results
                .first()
                .symbol,
        )
    }

    // --- Effect tests ---

    @Test
    fun `AssetClicked sends NavigateToDetail effect`() = runTest {
        viewModel = buildViewModel()
        viewModel.effects.test {
            viewModel.onEvent(SearchEvent.AssetClicked("SOL"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(SearchEffect.NavigateToDetail("SOL"), awaitItem())
        }
    }

    @Test
    fun `ToggleFavorite calls use case with correct args`() = runTest {
        coEvery { toggleFavorite("BTC", true) } returns Unit
        viewModel = buildViewModel()

        viewModel.onEvent(SearchEvent.ToggleFavorite("BTC", true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavorite("BTC", true) }
    }

    // --- Helpers ---

    private fun buildViewModel() = SearchViewModel(getWatchlist, toggleFavorite)

    private fun fakeAsset(
        symbol: String,
        name: String = symbol,
        pct: Double = 0.0,
        fav: Boolean = false,
        vol: Double = 50_000_000.0,
    ) = Asset(
        symbol = symbol,
        name = name,
        currentPrice = 100.0,
        priceChange24h = pct,
        priceChangePct24h = pct,
        marketCap = 1_000_000_000.0,
        volume24h = vol,
        logoUrl = null,
        isFavorite = fav,
        lastUpdated = 0L,
    )
}
