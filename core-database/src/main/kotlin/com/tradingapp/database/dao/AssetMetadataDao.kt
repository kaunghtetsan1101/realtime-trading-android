package com.tradingapp.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tradingapp.database.entity.AssetMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetMetadataDao {

    @Query("SELECT * FROM asset_metadata WHERE base_symbol = :symbol")
    fun observeBySymbol(symbol: String): Flow<AssetMetadataEntity?>

    @Query("SELECT * FROM asset_metadata")
    suspend fun getAll(): List<AssetMetadataEntity>

    @Query("SELECT * FROM asset_metadata WHERE base_symbol = :symbol")
    suspend fun getBySymbol(symbol: String): AssetMetadataEntity?

    @Upsert
    suspend fun upsertAll(entities: List<AssetMetadataEntity>)

    @Query("SELECT MAX(last_updated) FROM asset_metadata")
    suspend fun getLatestUpdateTime(): Long?
}
