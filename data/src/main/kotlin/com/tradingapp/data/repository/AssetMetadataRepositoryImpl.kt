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
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
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
        } catch (e: IOException) {
            // Network failure — Room data (possibly stale) remains available.
            Timber.w(e, "CoinGecko sync failed — using cached metadata")
        } catch (e: HttpException) {
            // HTTP error (4xx/5xx) — treat the same as a network failure.
            Timber.w(e, "CoinGecko sync failed with HTTP %d — using cached metadata", e.code())
        }
    }

    override fun observeMetadata(baseSymbol: String): Flow<AssetMetadata?> =
        metadataDao.observeBySymbol(baseSymbol).map { it?.toDomain() }

    companion object {
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 24 hours
    }
}
