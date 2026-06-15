package com.tradingapp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_metadata")
data class AssetMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "base_symbol")
    val baseSymbol: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long,
)
