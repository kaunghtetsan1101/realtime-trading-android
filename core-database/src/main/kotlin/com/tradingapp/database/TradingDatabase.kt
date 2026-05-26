package com.tradingapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.database.entity.AssetEntity

@Database(
    entities  = [AssetEntity::class],
    version   = 2,
    exportSchema = true,
)
abstract class TradingDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN high_24h REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE assets ADD COLUMN low_24h  REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
