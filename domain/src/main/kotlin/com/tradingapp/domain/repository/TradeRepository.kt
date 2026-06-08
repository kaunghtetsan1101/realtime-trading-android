package com.tradingapp.domain.repository

import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.Position
import kotlinx.coroutines.flow.Flow

interface TradeRepository {
    suspend fun placeOrder(order: Order, takeProfit: Double? = null, stopLoss: Double? = null): Result<Order>
    fun observeOrders(): Flow<List<Order>>
    fun observeOrdersForSymbol(symbol: String): Flow<List<Order>>
    fun observePositions(): Flow<List<Position>>
    fun observePosition(symbol: String): Flow<Position?>
    fun observePositionById(positionId: String): Flow<Position?>
    fun observeCashBalance(): Flow<Double>
    suspend fun getCashBalance(): Double
    suspend fun updatePositionRisk(positionId: String, takeProfit: Double?, stopLoss: Double?)
    suspend fun closePosition(positionId: String, closePrice: Double, reason: CloseReason): Result<Unit>
}
