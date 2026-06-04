package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.repository.TradeRepository
import java.util.UUID
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val tradeRepository: TradeRepository,
) {
    suspend operator fun invoke(
        side: OrderSide,
        symbol: String,
        quantity: Double,
        executionPrice: Double,
    ): Result<Order> = runCatching {
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = side,
            quantity = quantity,
            price = executionPrice,
            totalValue = quantity * executionPrice,
            status = OrderStatus.FILLED,
            timestamp = System.currentTimeMillis(),
        )
        tradeRepository.placeOrder(order).getOrThrow()
    }
}
