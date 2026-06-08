package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.repository.TradeRepository
import java.util.UUID
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(private val tradeRepository: TradeRepository) {
    suspend operator fun invoke(
        side: OrderSide,
        symbol: String,
        quantity: Double,
        executionPrice: Double,
        takeProfit: Double? = null,
        stopLoss: Double? = null,
    ): Result<Order> = runCatching {
        val direction = if (side == OrderSide.BUY) TradeDirection.LONG else TradeDirection.SHORT
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = side,
            direction = direction,
            quantity = quantity,
            price = executionPrice,
            totalValue = quantity * executionPrice,
            status = OrderStatus.FILLED,
            timestamp = System.currentTimeMillis(),
        )
        tradeRepository.placeOrder(order, takeProfit = takeProfit, stopLoss = stopLoss).getOrThrow()
    }
}
