package com.tradingapp.domain.model

data class Portfolio(
    val cashBalance: Double,
    val positions: List<Position>,
    val totalValue: Double,
    val totalUnrealizedPnL: Double,
    val totalUnrealizedPnLPct: Double,
)
