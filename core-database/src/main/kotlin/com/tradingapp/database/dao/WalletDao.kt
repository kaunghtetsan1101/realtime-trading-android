package com.tradingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradingapp.database.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    suspend fun get(): WalletEntity?

    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    fun observe(): Flow<WalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)
}
