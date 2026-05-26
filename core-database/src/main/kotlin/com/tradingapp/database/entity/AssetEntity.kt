package com.tradingapp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted representation of a market asset.
 *
 * [isFavorite] drives the watchlist persistence requirement.
 * [lastPrice] / [lastUpdated] provide offline cache data.
 */
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    @ColumnInfo(name = "symbol")        val symbol: String,
    @ColumnInfo(name = "name")          val name: String,
    @ColumnInfo(name = "price")         val price: Double,
    @ColumnInfo(name = "change_24h")    val change24h: Double,
    @ColumnInfo(name = "change_pct_24h") val changePct24h: Double,
    @ColumnInfo(name = "market_cap")    val marketCap: Double,
    @ColumnInfo(name = "volume_24h")    val volume24h: Double,
    @ColumnInfo(name = "high_24h")      val high24h: Double = 0.0,
    @ColumnInfo(name = "low_24h")       val low24h: Double = 0.0,
    @ColumnInfo(name = "logo_url")      val logoUrl: String?,
    @ColumnInfo(name = "is_favorite")   val isFavorite: Boolean = false,
    @ColumnInfo(name = "last_updated")  val lastUpdated: Long = System.currentTimeMillis(),
)
