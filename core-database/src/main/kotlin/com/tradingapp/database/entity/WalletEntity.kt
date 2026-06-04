package com.tradingapp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "cash_balance") val cashBalance: Double,
) {
    companion object {
        const val INITIAL_BALANCE = 10_000.0
    }
}
