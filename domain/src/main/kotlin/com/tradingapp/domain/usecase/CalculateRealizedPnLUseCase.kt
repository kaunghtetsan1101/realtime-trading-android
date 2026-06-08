package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.TradeDirection
import javax.inject.Inject

class CalculateRealizedPnLUseCase @Inject constructor() {
    operator fun invoke(
        direction: TradeDirection,
        entryPrice: Double,
        closePrice: Double,
        quantity: Double,
    ): Double = when (direction) {
        TradeDirection.LONG -> (closePrice - entryPrice) * quantity
        TradeDirection.SHORT -> (entryPrice - closePrice) * quantity
    }
}
