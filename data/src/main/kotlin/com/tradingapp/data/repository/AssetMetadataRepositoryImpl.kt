package com.tradingapp.data.repository

import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.mapper.toDomain
import com.tradingapp.data.mapper.toEntity
import com.tradingapp.data.provider.RoomBackedAssetMetadataProvider
import com.tradingapp.database.dao.AssetMetadataDao
import com.tradingapp.domain.model.AssetMetadata
import com.tradingapp.domain.repository.AssetMetadataRepository
import com.tradingapp.network.api.CoinGeckoApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetMetadataRepositoryImpl
@Inject
constructor(
    private val coinGeckoApi: CoinGeckoApi,
    private val metadataDao: AssetMetadataDao,
    private val provider: RoomBackedAssetMetadataProvider,
    private val dispatchers: DispatcherProvider,
) : AssetMetadataRepository {

    override suspend fun syncIfStale(): Unit = withContext(dispatchers.io) {
        // Always warm the in-memory cache from Room first — handles cold starts
        // where Room has data from a previous session but the in-memory map is empty.
        provider.refresh()

        val lastUpdated = metadataDao.getLatestUpdateTime() ?: 0L
        if ((System.currentTimeMillis() - lastUpdated) <= CACHE_TTL_MS) return@withContext

        try {
            val now = System.currentTimeMillis()
            val entities = coinGeckoApi.getMarkets().map { it.toEntity(now) }
            metadataDao.upsertAll(entities)
            provider.refresh()
            Timber.d("CoinGecko metadata synced — %d entries", entities.size)
        } catch (e: Exception) {
            // Price data from Binance is unaffected. Room data (possibly stale) remains available.
            Timber.w(e, "CoinGecko sync failed — using cached metadata")
        }
    }

    override fun observeMetadata(baseSymbol: String): Flow<AssetMetadata?> =
        metadataDao.observeBySymbol(baseSymbol).map { it?.toDomain() }

    companion object {
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 24 hours
    }
}
