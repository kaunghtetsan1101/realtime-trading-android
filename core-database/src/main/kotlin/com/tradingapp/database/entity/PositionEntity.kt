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
)
