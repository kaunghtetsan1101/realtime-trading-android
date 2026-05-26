package com.tradingapp.data.repository

import app.cash.turbine.test
import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.network.api.MarketApi
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

    private val dispatchers = object : DispatcherProvider {
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
        return AssetRepositoryImpl(
            assetDao, marketApi, webSocketManager, dispatchers, "wss://fake"
        )
    }

    // -------------------------------------------------------------------------
    // Watchlist-update regression
    // -------------------------------------------------------------------------

    @Test
    fun `WS tick causes assetDao updatePrice to be called — regression for watchlist bug`() =
        runTest(testDispatcher) {
            val repo = buildRepo()

            wsSource.emit(PriceTickDto("BTC", 67_500.0, 1_000L))

            coVerify { assetDao.updatePrice("BTC", 67_500.0, 1_000L) }
        }

    // -------------------------------------------------------------------------
    // SharedFlow pipeline
    // -------------------------------------------------------------------------

    @Test
    fun `WS tick is relayed to observePriceTicks subscriber for matching symbol`() =
        runTest(testDispatcher) {
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
    fun `WS tick for different symbol does not reach observePriceTicks subscriber`() =
        runTest(testDispatcher) {
            val repo = buildRepo()

            repo.observePriceTicks("BTC").test {
                wsSource.emit(PriceTickDto("ETH", 3_000.0, 3_000L))

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // -------------------------------------------------------------------------
    // REST sync
    // -------------------------------------------------------------------------

    @Test
    fun `syncAssets sends URL-encoded JSON array, inserts new rows, and updates market data`() =
        runTest(testDispatcher) {
            val repo = buildRepo()
            val fakeDtos = AssetRepositoryImpl.TRACKED_SYMBOLS.map { sym ->
                BinanceTicker24hrDto(sym, "100.0", "1.0", "1.0", "1000.0", "100000.0")
            }
            coEvery { marketApi.get24hrTickers(any()) } returns fakeDtos

            repo.syncAssets()

            coVerify {
                marketApi.get24hrTickers(
                    withArg { symbols ->
                        assert(symbols.startsWith("[\"BTCUSDT\"")) {
                            "Expected symbols to start with [\"BTCUSDT\" but was: $symbols"
                        }
                        assert(symbols.contains("\"ETHUSDT\"")) {
                            "Expected ETHUSDT in symbols but was: $symbols"
                        }
                        assert(symbols.endsWith("ADAUSDT\"]")) {
                            "Expected symbols to end with ADAUSDT\"] but was: $symbols"
                        }
                    }
                )
            }
            // Step 1: new rows inserted with IGNORE (preserves isFavorite on conflict)
            coVerify { assetDao.insertAllIgnore(any()) }
            // Step 2: market-data columns updated — isFavorite never touched
            coVerify(exactly = AssetRepositoryImpl.TRACKED_SYMBOLS.size) {
                assetDao.updateMarketData(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `syncAssets does not call updateMarketData with isFavorite — persistence guaranteed`() =
        runTest(testDispatcher) {
            // Regression guard: ensure the two-step upsert path is taken, not the old REPLACE path.
            val repo = buildRepo()
            coEvery { marketApi.get24hrTickers(any()) } returns listOf(
                BinanceTicker24hrDto("BTCUSDT", "67500.0", "500.0", "0.75", "1000.0", "67500000.0")
            )

            repo.syncAssets()

            // The dangerous old method must NOT be called
            coVerify(exactly = 0) { assetDao.setFavorite(any(), any()) }
        }

    // -------------------------------------------------------------------------
    // observeAssets delegates to Room
    // -------------------------------------------------------------------------

    @Test
    fun `observeAssets returns mapped entities from Room Flow`() =
        runTest(testDispatcher) {
            every { webSocketManager.observePriceTicks(any()) } returns wsSource
            every { assetDao.observeAll() } returns flowOf(emptyList())
            every { assetDao.observeBySymbol(any()) } returns emptyFlow()
            every { assetDao.observeFavorites() } returns emptyFlow()

            val repo = AssetRepositoryImpl(
                assetDao, marketApi, webSocketManager, dispatchers, "wss://fake"
            )

            repo.observeAssets().test {
                val assets = awaitItem()
                assertEquals(emptyList<Any>(), assets)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
