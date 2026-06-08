package com.tradingapp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "side") val side: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "total_value") val totalValue: Double,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "direction") val direction: String = "LONG",
    @ColumnInfo(name = "close_price") val closePrice: Double? = null,
    @ColumnInfo(name = "closed_at") val closedAt: Long? = null,
    @ColumnInfo(name = "close_reason") val closeReason: String? = null,
    @ColumnInfo(name = "realized_pnl") val realizedPnL: Double? = null,
)
