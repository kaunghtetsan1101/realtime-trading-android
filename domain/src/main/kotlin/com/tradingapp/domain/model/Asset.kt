package com.tradingapp.domain.model

/**
 * Pure domain model — no framework imports, no serialization annotations.
 *
 * [priceChange24h] and [priceChangePct24h] are signed: positive = up, negative = down.
 */
data class Asset(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePct24h: Double,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val marketCap: Double,
    val volume24h: Double,
    val logoUrl: String?,
    val isFavorite: Boolean,
    val lastUpdated: Long,
) {
    val isUp: Boolean get() = priceChange24h >= 0.0
}
