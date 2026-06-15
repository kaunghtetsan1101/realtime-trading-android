package com.tradingapp.data.repository

import app.cash.turbine.test
import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.provider.StaticAssetMetadataProvider
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.network.api.MarketApi
import com.tradingapp.network.model.BinanceMiniTickerDto
import com.tradingapp.network.model.BinanceTicker24hrDto
import com.tradingapp.network.model.PriceTickDto
import com.tradingapp.network.websocket.WebSocketManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssetRepositoryImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dispatchers =
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }

    private val assetDao = mockk<AssetDao>(relaxed = true)
    private val marketApi = mockk<MarketApi>(relaxed = true)
    private val webSocketManager = mockk<WebSocketManager>()

    // Shared flow acts as a test WS source — emit to it to simulate incoming ticks.
    private val wsSource = MutableSharedFlow<PriceTickDto>(replay = 0, extraBufferCapacity = 8)

    private fun buildRepo(): AssetRepositoryImpl {
        every { webSocketManager.observePriceTicks(any()) } returns wsSource
        every { assetDao.observeAll() } returns emptyFlow()
        every { assetDao.observeBySymbol(any()) } returns emptyFlow()
        every { assetDao.observeFavorites() } returns emptyFlow()
        // DB empty on first launch — repo falls back to BOOTSTRAP_SYMBOLS for initial WS URL.
        coEvery { assetDao.getTopSymbols(any()) } returns emptyList()
        return AssetRepositoryImpl(assetDao, marketApi, webSocketManager, dispatchers, StaticAssetMetadataProvider())
    }

    // -------------------------------------------------------------------------
    // Watchlist-update regression
    // -------------------------------------------------------------------------

    @Test
    fun `WS tick causes assetDao updatePrice to be called — regression for watchlist bug`() = runTest(testDispatcher) {
        val repo = buildRepo()

        wsSource.emit(PriceTickDto("BTC", 67_500.0, 1_000L))

        coVerify { assetDao.updatePrice("BTC", 67_500.0, 1_000L) }
    }

    // -------------------------------------------------------------------------
    // SharedFlow pipeline
    // -------------------------------------------------------------------------

    @Test
    fun `WS tick is relayed to observePriceTicks subscriber for matching symbol`() = runTest(testDispatcher) {
        val repo = buildRepo()

        repo.observePriceTicks("BTC").test {
            wsSource.emit(PriceTickDto("BTC", 67_500.0, 2_000L))

            val tick = awaitItem()
            assertEquals("BTC", tick.symbol)
            assertEquals(67_500.0, tick.price, 0.001)
            assertEquals(2_000L, tick.timestamp)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WS tick for different symbol does not reach observePriceTicks subscriber`() = runTest(testDispatcher) {
        val repo = buildRepo()

        repo.observePriceTicks("BTC").test {
            wsSource.emit(PriceTickDto("ETH", 3_000.0, 3_000L))

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // REST sync — two-step discovery
    // -------------------------------------------------------------------------

    @Test
    fun `syncAssets discovers USDT pairs by volume, fetches full tickers, and upserts Room`() =
        runTest(testDispatcher) {
            val repo = buildRepo()
            val miniTickers =
                listOf(
                    BinanceMiniTickerDto("BTCUSDT", "1000000.0"),
                    BinanceMiniTickerDto("ETHUSDT", "500000.0"),
                    BinanceMiniTickerDto("ETHBTC", "100000.0"), // non-USDT — must be filtered out
                )
            val fullTickers =
                listOf(
                    fakeTicker("BTCUSDT"),
                    fakeTicker("ETHUSDT"),
                )
            coEvery { marketApi.getAllMiniTickers() } returns miniTickers
            coEvery { marketApi.get24hrTickers(any()) } returns fullTickers

            repo.syncAssets()

            // Discovery call made
            coVerify { marketApi.getAllMiniTickers() }
            // Full-data call includes USDT pairs only
            coVerify {
                marketApi.get24hrTickers(
                    withArg { json ->
                        assert(json.contains("BTCUSDT")) { "BTCUSDT expected in $json" }
                        assert(json.contains("ETHUSDT")) { "ETHUSDT expected in $json" }
                        assert(!json.contains("ETHBTC")) { "ETHBTC should be filtered; found in $json" }
                    },
                )
            }
            // DB upsert executed for both symbols
            coVerify { assetDao.insertAllIgnore(any()) }
            coVerify(exactly = 2) {
                assetDao.updateMarketData(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `syncAssets does not call setFavorite — isFavorite persistence guaranteed`() = runTest(testDispatcher) {
        val repo = buildRepo()
        coEvery { marketApi.getAllMiniTickers() } returns
            listOf(
                BinanceMiniTickerDto("BTCUSDT", "1000000.0"),
            )
        coEvery { marketApi.get24hrTickers(any()) } returns listOf(fakeTicker("BTCUSDT"))

        repo.syncAssets()

        // The dangerous REPLACE path must never be taken
        coVerify(exactly = 0) { assetDao.setFavorite(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // toggleFavorite persistence
    // -------------------------------------------------------------------------

    @Test
    fun `toggleFavorite delegates to assetDao setFavorite with correct arguments`() = runTest(testDispatcher) {
        val repo = buildRepo()

        repo.toggleFavorite("BTC", true)

        coVerify { assetDao.setFavorite("BTC", true) }
    }

    @Test
    fun `toggleFavorite unfavorite delegates to assetDao setFavorite with false`() = runTest(testDispatcher) {
        val repo = buildRepo()

        repo.toggleFavorite("ETH", false)

        coVerify { assetDao.setFavorite("ETH", false) }
    }

    // -------------------------------------------------------------------------
    // observeAssets delegates to Room
    // -------------------------------------------------------------------------

    @Test
    fun `observeAssets returns mapped entities from Room Flow`() = runTest(testDispatcher) {
        every { webSocketManager.observePriceTicks(any()) } returns wsSource
        every { assetDao.observeAll() } returns flowOf(emptyList())
        every { assetDao.observeBySymbol(any()) } returns emptyFlow()
        every { assetDao.observeFavorites() } returns emptyFlow()
        coEvery { assetDao.getTopSymbols(any()) } returns emptyList()

        val repo = AssetRepositoryImpl(
            assetDao,
            marketApi,
            webSocketManager,
            dispatchers,
            StaticAssetMetadataProvider(),
        )

        repo.observeAssets().test {
            val assets = awaitItem()
            assertEquals(emptyList<Any>(), assets)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helpers ---

    private fun fakeTicker(symbol: String) = BinanceTicker24hrDto(
        symbol = symbol,
        lastPrice = "100.0",
        priceChange = "1.0",
        priceChangePercent = "1.0",
        highPrice = "105.0",
        lowPrice = "95.0",
        volume = "1000.0",
        quoteVolume = "100000.0",
    )
}
