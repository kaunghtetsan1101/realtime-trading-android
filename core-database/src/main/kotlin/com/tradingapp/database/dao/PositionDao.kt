package com.tradingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradingapp.database.entity.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions")
    fun observeAll(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    fun observeBySymbol(symbol: String): Flow<PositionEntity?>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PositionEntity)

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)
}
