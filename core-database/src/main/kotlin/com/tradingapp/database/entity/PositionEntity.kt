package com.tradingapp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "avg_price") val avgPrice: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "id") val id: String = "",
    @ColumnInfo(name = "direction") val direction: String = "LONG",
    @ColumnInfo(name = "take_profit") val takeProfit: Double? = null,
    @ColumnInfo(name = "stop_loss") val stopLoss: Double? = null,
    @ColumnInfo(name = "opened_at") val openedAt: Long = 0L,
)
