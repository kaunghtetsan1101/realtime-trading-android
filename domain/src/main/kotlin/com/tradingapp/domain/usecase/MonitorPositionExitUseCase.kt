package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.TradeDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class MonitorPositionExitUseCase @Inject constructor() {
    operator fun invoke(position: Position, priceFlow: Flow<Double>): Flow<CloseReason> =
        priceFlow
            .distinctUntilChanged()
            .mapNotNull { price -> checkExit(position, price) }

    private fun checkExit(position: Position, price: Double): CloseReason? {
        val tp = position.takeProfit
        val sl = position.stopLoss
        return when (position.direction) {
            TradeDirection.LONG -> when {
                tp != null && price >= tp -> CloseReason.TAKE_PROFIT_TRIGGERED
                sl != null && price <= sl -> CloseReason.STOP_LOSS_TRIGGERED
                else -> null
            }
            TradeDirection.SHORT -> when {
                tp != null && price <= tp -> CloseReason.TAKE_PROFIT_TRIGGERED
                sl != null && price >= sl -> CloseReason.STOP_LOSS_TRIGGERED
                else -> null
            }
        }
    }
}
