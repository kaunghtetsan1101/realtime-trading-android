package com.tradingapp.domain.model

data class Position(
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double = 0.0,
    val totalValue: Double = 0.0,
    val unrealizedPnL: Double = 0.0,
    val unrealizedPnLPct: Double = 0.0,
)
