package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.repository.TradeRepository
import javax.inject.Inject

class EditPositionRiskUseCase @Inject constructor(
    private val tradeRepository: TradeRepository,
    private val validateTpSl: ValidateTakeProfitStopLossUseCase,
) {
    suspend operator fun invoke(
        positionId: String,
        direction: TradeDirection,
        entryPrice: Double,
        takeProfitStr: String,
        stopLossStr: String,
    ): Result<Unit> {
        val validation = validateTpSl(direction, entryPrice, takeProfitStr, stopLossStr)
        if (validation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.error.name))
        }
        val tp = takeProfitStr.toDoubleOrNull()
        val sl = stopLossStr.toDoubleOrNull()
        return runCatching { tradeRepository.updatePositionRisk(positionId, tp, sl) }
    }
}
