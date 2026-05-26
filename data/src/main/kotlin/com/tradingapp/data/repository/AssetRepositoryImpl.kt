package com.tradingapp.data.repository

import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.di.WsUrl
import com.tradingapp.data.mapper.toDomain
import com.tradingapp.data.mapper.toEntity
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.repository.AssetRepository
import com.tradingapp.network.api.MarketApi
import com.tradingapp.network.model.PriceTickDto
import com.tradingapp.network.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
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
 * WS pipeline (opened ONCE in init, not per subscriber):
 *   WebSocket tick → _priceTicks SharedFlow + Room.updatePrice() → observeAssets() re-emits
 *
 * This fixes the original bug where [observeAssets] never reflected live prices because
 * nobody was collecting [observePriceTicks] to drive the Room writes.
 *
 * Design decisions:
 * - [_priceTicks] replay=1 + DROP_OLDEST: detail screen gets an immediate value on open,
 *   backpressure never stalls the WS pump.
 * - [SupervisorJob]: a WS error doesn't cancel unrelated repository coroutines.
 * - retryWhen with capped exponential backoff: handles Binance WS disconnects gracefully.
 */
@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val marketApi: MarketApi,
    private val webSocketManager: WebSocketManager,
    private val dispatchers: DispatcherProvider,
    @WsUrl private val wsUrl: String,
) : AssetRepository {

    private val _priceTicks = MutableSharedFlow<PriceTickDto>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val repositoryScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        repositoryScope.launch {
            webSocketManager.observePriceTicks(wsUrl)
                .retryWhen { _, attempt ->
                    val backoffMs = min(
                        (1L shl attempt.toInt().coerceAtMost(14)) * 2_000L,
                        30_000L,
                    )
                    delay(backoffMs)
                    true
                }
                .collect { tick ->
                    _priceTicks.emit(tick)
                    assetDao.updatePrice(tick.symbol, tick.price, tick.timestamp)
                }
        }
    }

    override fun observeAssets(): Flow<List<Asset>> =
        assetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeAsset(symbol: String): Flow<Asset?> =
        assetDao.observeBySymbol(symbol).map { it?.toDomain() }

    override fun observeFavorites(): Flow<List<Asset>> =
        assetDao.observeFavorites().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncAssets(): Result<Unit> = runCatching {
        withContext(dispatchers.io) {
            val symbolsJson = "[" + TRACKED_SYMBOLS.joinToString(",") { "\"$it\"" } + "]"
            val entities = marketApi.get24hrTickers(symbolsJson).map { it.toEntity() }
            // Step 1: insert rows that are genuinely new (IGNORE = no-op if already in DB).
            assetDao.insertAllIgnore(entities)
            // Step 2: refresh market-data columns only — isFavorite is never touched.
            entities.forEach { e ->
                assetDao.updateMarketData(
                    symbol       = e.symbol,
                    name         = e.name,
                    price        = e.price,
                    change24h    = e.change24h,
                    changePct24h = e.changePct24h,
                    marketCap    = e.marketCap,
                    volume24h    = e.volume24h,
                    logoUrl      = e.logoUrl,
                )
            }
        }
    }

    override suspend fun toggleFavorite(symbol: String, isFavorite: Boolean) {
        withContext(dispatchers.io) {
            assetDao.setFavorite(symbol, isFavorite)
        }
    }

    override fun observePriceTicks(symbol: String): Flow<PriceTick> =
        _priceTicks
            .filter { it.symbol == symbol }
            .map { it.toDomain() }

    companion object {
        /** Binance USDT-pair symbols tracked across the app — single source of truth. */
        val TRACKED_SYMBOLS = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "ADAUSDT")
    }
}
