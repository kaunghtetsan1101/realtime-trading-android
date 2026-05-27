package com.tradingapp.domain.repository

import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import kotlinx.coroutines.flow.Flow

/**
 * Contract between the domain layer and data layer.
 *
 * All Flow-returning functions are cold — they begin work when collected.
 * [observeAssets] is the primary watchlist source; it merges cached DB data
 * with live WebSocket ticks.
 */
interface AssetRepository {
    /** Emits the full asset list, updating on every price tick and DB change. */
    fun observeAssets(): Flow<List<Asset>>

    /** Emits a single asset by symbol, or null if not found. */
    fun observeAsset(symbol: String): Flow<Asset?>

    /** Emits only favorited assets. */
    fun observeFavorites(): Flow<List<Asset>>

    /** Seed / refresh asset catalog from the remote source. */
    suspend fun syncAssets(): Result<Unit>

    /** Toggle the favorite flag for a given symbol. */
    suspend fun toggleFavorite(symbol: String, isFavorite: Boolean)

    /** Observe live price ticks directly (used by detail screen). */
    fun observePriceTicks(symbol: String): Flow<PriceTick>
}
