package com.tradingapp.domain.model

enum class OrderSide { BUY, SELL }

enum class OrderStatus { FILLED, CANCELLED }

data class Order(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Double,
    val price: Double,
    val totalValue: Double,
    val status: OrderStatus,
    val timestamp: Long,
)
