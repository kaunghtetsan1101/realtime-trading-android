package com.tradingapp.domain.model

data class PriceTick(
    val symbol: String,
    val price: Double,
    val timestamp: Long,
)
