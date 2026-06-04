package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.repository.TradeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOrderHistoryUseCase @Inject constructor(
    private val tradeRepository: TradeRepository,
) {
    operator fun invoke(symbol: String? = null): Flow<List<Order>> = if (symbol != null) {
        tradeRepository.observeOrdersForSymbol(symbol)
    } else {
        tradeRepository.observeOrders()
    }
}
