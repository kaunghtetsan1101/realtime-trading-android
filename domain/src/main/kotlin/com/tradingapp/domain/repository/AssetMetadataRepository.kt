package com.tradingapp.domain.repository

import com.tradingapp.domain.model.AssetMetadata
import kotlinx.coroutines.flow.Flow

interface AssetMetadataRepository {
    /**
     * Warms the in-memory cache from Room, then fetches fresh data from CoinGecko
     * if the local cache is older than the configured TTL. Safe to call on every
     * app start — it is a no-op when data is fresh.
     */
    suspend fun syncIfStale()

    fun observeMetadata(baseSymbol: String): Flow<AssetMetadata?>
}
