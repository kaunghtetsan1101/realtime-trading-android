package com.tradingapp.domain.model

data class Position(
    val id: String,
    val symbol: String,
    val direction: TradeDirection,
    val quantity: Double,
    val averagePrice: Double,
    val takeProfit: Double? = null,
    val stopLoss: Double? = null,
    val openedAt: Long = 0L,
    // Enriched at use-case layer, not persisted:
    val currentPrice: Double = 0.0,
    val totalValue: Double = 0.0,
    val unrealizedPnL: Double = 0.0,
    val unrealizedPnLPct: Double = 0.0,
)
