package com.tradingapp.data.mapper

import com.tradingapp.database.entity.AssetMetadataEntity
import com.tradingapp.domain.model.AssetMetadata
import com.tradingapp.network.model.CoinGeckoMarketDto

fun CoinGeckoMarketDto.toEntity(timestamp: Long): AssetMetadataEntity = AssetMetadataEntity(
    baseSymbol = symbol.uppercase(),
    displayName = name,
    imageUrl = image,
    lastUpdated = timestamp,
)

fun AssetMetadataEntity.toDomain(): AssetMetadata = AssetMetadata(
    baseSymbol = baseSymbol,
    displayName = displayName,
    imageUrl = imageUrl,
    lastUpdated = lastUpdated,
)
