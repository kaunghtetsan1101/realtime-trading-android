package com.tradingapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tradingapp.database.dao.AssetDao
import com.tradingapp.database.dao.AssetMetadataDao
import com.tradingapp.database.dao.OrderDao
import com.tradingapp.database.dao.PositionDao
import com.tradingapp.database.dao.WalletDao
import com.tradingapp.database.entity.AssetEntity
import com.tradingapp.database.entity.AssetMetadataEntity
import com.tradingapp.database.entity.OrderEntity
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.database.entity.WalletEntity

@Database(
    entities = [
        AssetEntity::class,
        AssetMetadataEntity::class,
        OrderEntity::class,
        PositionEntity::class,
        WalletEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class TradingDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun assetMetadataDao(): AssetMetadataDao
    abstract fun orderDao(): OrderDao
    abstract fun positionDao(): PositionDao
    abstract fun walletDao(): WalletDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE assets ADD COLUMN high_24h REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE assets ADD COLUMN low_24h  REAL NOT NULL DEFAULT 0.0")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS orders (
                            id TEXT PRIMARY KEY NOT NULL,
                            symbol TEXT NOT NULL,
                            side TEXT NOT NULL,
                            quantity REAL NOT NULL,
                            price REAL NOT NULL,
                            total_value REAL NOT NULL,
                            status TEXT NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS positions (
                            symbol TEXT PRIMARY KEY NOT NULL,
                            quantity REAL NOT NULL,
                            avg_price REAL NOT NULL,
                            updated_at INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS wallet (
                            id INTEGER PRIMARY KEY NOT NULL,
                            cash_balance REAL NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("INSERT INTO wallet (id, cash_balance) VALUES (1, 10000.0)")
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE positions ADD COLUMN id TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE positions ADD COLUMN direction TEXT NOT NULL DEFAULT 'LONG'")
                    db.execSQL("ALTER TABLE positions ADD COLUMN take_profit REAL")
                    db.execSQL("ALTER TABLE positions ADD COLUMN stop_loss REAL")
                    db.execSQL("ALTER TABLE positions ADD COLUMN opened_at INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE orders ADD COLUMN direction TEXT NOT NULL DEFAULT 'LONG'")
                    db.execSQL("ALTER TABLE orders ADD COLUMN close_price REAL")
                    db.execSQL("ALTER TABLE orders ADD COLUMN closed_at INTEGER")
                    db.execSQL("ALTER TABLE orders ADD COLUMN close_reason TEXT")
                    db.execSQL("ALTER TABLE orders ADD COLUMN realized_pnl REAL")
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS asset_metadata (
                            base_symbol  TEXT PRIMARY KEY NOT NULL,
                            display_name TEXT NOT NULL,
                            image_url    TEXT,
                            last_updated INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
