package com.tradingapp.data.provider

import com.tradingapp.data.mapper.toDomain
import com.tradingapp.database.dao.AssetMetadataDao
import com.tradingapp.domain.model.AssetMetadata
import com.tradingapp.domain.provider.AssetMetadataProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous metadata provider backed by an in-memory snapshot of Room.
 *
 * [refresh] must be called at app startup (and after each CoinGecko sync) to
 * populate the snapshot. Between refreshes the map is read lock-free via
 * [AtomicReference], so concurrent reads from the Binance sync hot-path are safe.
 *
 * Unknown symbols return a fallback with the symbol as the display name and a null
 * image URL — the UI [AssetIcon] handles the null by showing an initials avatar.
 */
@Singleton
class RoomBackedAssetMetadataProvider
@Inject
constructor(private val metadataDao: AssetMetadataDao) : AssetMetadataProvider {

    private val snapshot = AtomicReference<Map<String, AssetMetadata>>(emptyMap())

    suspend fun refresh() {
        val all = metadataDao.getAll()
        snapshot.set(all.associate { it.baseSymbol to it.toDomain() })
    }

    override fun getMetadata(baseSymbol: String): AssetMetadata =
        snapshot.get()[baseSymbol] ?: AssetMetadata(
            baseSymbol = baseSymbol,
            displayName = baseSymbol,
            imageUrl = null,
        )
}
