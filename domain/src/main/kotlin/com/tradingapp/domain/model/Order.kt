package com.tradingapp.domain.model

enum class OrderSide { BUY, SELL }

enum class OrderStatus { OPEN, FILLED, CANCELLED }

data class Order(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val direction: TradeDirection,
    val quantity: Double,
    val price: Double,
    val totalValue: Double,
    val status: OrderStatus,
    val timestamp: Long,
    val closePrice: Double? = null,
    val closedAt: Long? = null,
    val closeReason: CloseReason? = null,
    val realizedPnL: Double? = null,
)
