package com.tradingapp.data.mapper

import com.tradingapp.database.entity.AssetEntity
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.network.model.BinanceTicker24hrDto
import com.tradingapp.network.model.PriceTickDto

// --- DTO → Entity ---

private val NAME_MAP = mapOf(
    "BTC" to "Bitcoin",
    "ETH" to "Ethereum",
    "SOL" to "Solana",
    "BNB" to "BNB",
    "ADA" to "Cardano",
)

fun BinanceTicker24hrDto.toEntity(): AssetEntity {
    val bare = symbol.removeSuffix("USDT")
    return AssetEntity(
        symbol       = bare,
        name         = NAME_MAP[bare] ?: bare,
        price        = lastPrice.toDoubleOrNull() ?: 0.0,
        change24h    = priceChange.toDoubleOrNull() ?: 0.0,
        changePct24h = priceChangePercent.toDoubleOrNull() ?: 0.0,
        high24h      = highPrice.toDoubleOrNull() ?: 0.0,
        low24h       = lowPrice.toDoubleOrNull() ?: 0.0,
        // Binance public API has no market cap; use quoteVolume as a proxy
        marketCap    = quoteVolume.toDoubleOrNull() ?: 0.0,
        volume24h    = volume.toDoubleOrNull() ?: 0.0,
        logoUrl      = "https://assets.coincap.io/assets/icons/${bare.lowercase()}@2x.png",
    )
}

// --- Entity → Domain ---

fun AssetEntity.toDomain(): Asset = Asset(
    symbol            = symbol,
    name              = name,
    currentPrice      = price,
    priceChange24h    = change24h,
    priceChangePct24h = changePct24h,
    high24h           = high24h,
    low24h            = low24h,
    marketCap         = marketCap,
    volume24h         = volume24h,
    logoUrl           = logoUrl,
    isFavorite        = isFavorite,
    lastUpdated       = lastUpdated,
)

// --- Network tick → Domain tick ---

fun PriceTickDto.toDomain(): PriceTick = PriceTick(
    symbol    = symbol,
    price     = price,
    timestamp = timestamp,
)
