package com.tradingapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.database.entity.AssetEntity

@Database(
    entities  = [AssetEntity::class],
    version   = 1,
    exportSchema = true,
)
abstract class TradingDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
}
