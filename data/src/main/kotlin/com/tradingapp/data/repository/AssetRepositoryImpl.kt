package com.tradingapp.data.repository

import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.mapper.toDomain
import com.tradingapp.data.mapper.toEntity
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.provider.AssetMetadataProvider
import com.tradingapp.domain.repository.AssetRepository
import com.tradingapp.network.api.MarketApi
import com.tradingapp.network.model.PriceTickDto
import com.tradingapp.network.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Single source of truth: Room DB.
 *
 * Symbol discovery (two-step sync):
 *   1. GET /api/v3/ticker/24hr?type=MINI  — fetches all pairs cheaply, filters USDT,
 *      sorts by quoteVolume, takes top [MAX_TRACKED].
 *   2. GET /api/v3/ticker/24hr?symbols=[…] — fetches full 24h stats for those symbols.
 *
 * WebSocket pipeline:
 *   - Driven by [wsUrlMutable]: a null-initialized StateFlow set to a real URL after the
 *     first DB read (startup) or after each sync.
 *   - [flatMapLatest] restarts the WebSocket whenever the URL changes, so the live-tick
 *     subscription always matches the current top-[MAX_WS_STREAMS] symbols.
 *   - [priceTicksMutable] replay=1 + DROP_OLDEST: detail screen gets an immediate value on
 *     open; backpressure never stalls the WS pump.
 *   - SupervisorJob: a WS error does not cancel unrelated repository coroutines.
 *   - retryWhen with capped exponential backoff handles Binance WS disconnects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val marketApi: MarketApi,
    private val webSocketManager: WebSocketManager,
    private val dispatchers: DispatcherProvider,
    private val metadataProvider: AssetMetadataProvider,
) : AssetRepository {

    private val priceTicksMutable = MutableSharedFlow<PriceTickDto>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val repositoryScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Null until the initial DB read completes; set to a real URL before the WS starts.
    private val wsUrlMutable = MutableStateFlow<String?>(null)

    init {
        // Build the initial WS URL from cached DB data so live ticks start immediately
        // on subsequent launches without waiting for a network sync.
        repositoryScope.launch {
            val dbSymbols = assetDao.getTopSymbols(MAX_WS_STREAMS)
            wsUrlMutable.value = if (dbSymbols.isNotEmpty()) {
                buildWsUrl(dbSymbols.map { "${it}USDT" })
            } else {
                buildWsUrl(BOOTSTRAP_SYMBOLS)
            }
        }

        // flatMapLatest: whenever wsUrlMutable emits a new URL the old WebSocket is cancelled
        // and a new one opens. This keeps subscriptions in sync after each syncAssets() call.
        repositoryScope.launch {
            wsUrlMutable
                .filterNotNull()
                .distinctUntilChanged()
                .flatMapLatest { url ->
                    webSocketManager.observePriceTicks(url)
                        .retryWhen { _, attempt ->
                            val backoffMs = min(
                                (1L shl attempt.toInt().coerceAtMost(WS_BACKOFF_MAX_SHIFT)) * WS_BACKOFF_BASE_MS,
                                WS_BACKOFF_MAX_MS,
                            )
                            delay(backoffMs)
                            true
                        }
                }
                .collect { tick ->
                    priceTicksMutable.emit(tick)
                    assetDao.updatePrice(tick.symbol, tick.price, tick.timestamp)
                }
        }
    }

    override fun observeAssets(): Flow<List<Asset>> =
        assetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeAsset(symbol: String): Flow<Asset?> = assetDao.observeBySymbol(symbol).map { it?.toDomain() }

    override fun observeFavorites(): Flow<List<Asset>> =
        assetDao.observeFavorites().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncAssets(): Result<Unit> = runCatching {
        withContext(dispatchers.io) {
            // Step 1: Discover active USDT pairs across all of Binance, ranked by 24h volume.
            val topSymbols = marketApi.getAllMiniTickers()
                .filter { it.symbol.endsWith("USDT") }
                .sortedByDescending { it.quoteVolume.toDoubleOrNull() ?: 0.0 }
                .take(MAX_TRACKED)
                .map { it.symbol }

            // Step 2: Fetch full 24h statistics (priceChange%, high, low, …) for those symbols.
            val symbolsJson = "[" + topSymbols.joinToString(",") { "\"$it\"" } + "]"
            val entities = marketApi.get24hrTickers(symbolsJson).map { it.toEntity(metadataProvider) }

            // Step 3: Two-step upsert — isFavorite is never touched.
            assetDao.insertAllIgnore(entities)
            entities.forEach { e ->
                assetDao.updateMarketData(
                    symbol = e.symbol,
                    name = e.name,
                    price = e.price,
                    change24h = e.change24h,
                    changePct24h = e.changePct24h,
                    high24h = e.high24h,
                    low24h = e.low24h,
                    marketCap = e.marketCap,
                    volume24h = e.volume24h,
                    logoUrl = e.logoUrl,
                )
            }

            // Step 4: Reconnect WebSocket to the new top symbols.
            wsUrlMutable.value = buildWsUrl(topSymbols.take(MAX_WS_STREAMS))
        }
    }

    override suspend fun toggleFavorite(symbol: String, isFavorite: Boolean) {
        withContext(dispatchers.io) {
            assetDao.setFavorite(symbol, isFavorite)
        }
    }

    override fun observePriceTicks(symbol: String): Flow<PriceTick> = priceTicksMutable
        .filter { it.symbol == symbol }
        .map { it.toDomain() }

    // --- Private ---

    private fun buildWsUrl(symbols: List<String>): String {
        require(symbols.isNotEmpty()) { "Cannot build WS URL from an empty symbol list" }
        val streams = symbols.joinToString("/") { "${it.lowercase()}@miniTicker" }
        return "wss://stream.binance.com:9443/stream?streams=$streams"
    }

    companion object {
        /** Symbols stored in Room and visible in the watchlist. */
        const val MAX_TRACKED = 100

        /**
         * Symbols with live WebSocket price ticks.
         *
         * Capped at 50 — all well within Binance's 1 024-stream combined-stream limit.
         * Assets ranked 51–100 in Room still display their last-synced price; they just
         * don't receive real-time ticks.
         */
        const val MAX_WS_STREAMS = 50

        /**
         * Bootstrap WebSocket symbols used on the very first launch before any sync has run
         * and the DB is still empty.
         */
        val BOOTSTRAP_SYMBOLS = listOf(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "MATICUSDT",
        )

        /** Exponential backoff: base delay before each WS reconnect attempt. */
        private const val WS_BACKOFF_BASE_MS = 2_000L

        /** Exponential backoff: maximum delay cap for WS reconnect. */
        private const val WS_BACKOFF_MAX_MS = 30_000L

        /** Exponential backoff: bit-shift cap so delay never overflows Long. */
        private const val WS_BACKOFF_MAX_SHIFT = 14
    }
}
