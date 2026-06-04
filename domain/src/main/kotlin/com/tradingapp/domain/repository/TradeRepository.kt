package com.tradingapp.domain.repository

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.Position
import kotlinx.coroutines.flow.Flow

interface TradeRepository {
    suspend fun placeOrder(order: Order): Result<Order>
    fun observeOrders(): Flow<List<Order>>
    fun observeOrdersForSymbol(symbol: String): Flow<List<Order>>
    fun observePositions(): Flow<List<Position>>
    fun observePosition(symbol: String): Flow<Position?>
    fun observeCashBalance(): Flow<Double>
    suspend fun getCashBalance(): Double
}
