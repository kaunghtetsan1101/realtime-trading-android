package com.tradingapp.data.mapper

import com.tradingapp.database.entity.AssetEntity
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.provider.AssetMetadataProvider
import com.tradingapp.network.model.BinanceTicker24hrDto
import com.tradingapp.network.model.PriceTickDto

// --- DTO → Entity ---

fun BinanceTicker24hrDto.toEntity(provider: AssetMetadataProvider): AssetEntity {
    val bare = symbol.removeSuffix("USDT")
    val metadata = provider.getMetadata(bare)
    return AssetEntity(
        symbol = bare,
        name = metadata.displayName,
        price = lastPrice.toDoubleOrNull() ?: 0.0,
        change24h = priceChange.toDoubleOrNull() ?: 0.0,
        changePct24h = priceChangePercent.toDoubleOrNull() ?: 0.0,
        high24h = highPrice.toDoubleOrNull() ?: 0.0,
        low24h = lowPrice.toDoubleOrNull() ?: 0.0,
        // Binance public API has no market cap; use quoteVolume as a proxy
        marketCap = quoteVolume.toDoubleOrNull() ?: 0.0,
        volume24h = volume.toDoubleOrNull() ?: 0.0,
        logoUrl = metadata.imageUrl,
    )
}

// --- Entity → Domain ---

fun AssetEntity.toDomain(): Asset = Asset(
    symbol = symbol,
    name = name,
    currentPrice = price,
    priceChange24h = change24h,
    priceChangePct24h = changePct24h,
    high24h = high24h,
    low24h = low24h,
    marketCap = marketCap,
    volume24h = volume24h,
    logoUrl = logoUrl,
    isFavorite = isFavorite,
    lastUpdated = lastUpdated,
)

// --- Network tick → Domain tick ---

fun PriceTickDto.toDomain(): PriceTick = PriceTick(
    symbol = symbol,
    price = price,
    timestamp = timestamp,
)
