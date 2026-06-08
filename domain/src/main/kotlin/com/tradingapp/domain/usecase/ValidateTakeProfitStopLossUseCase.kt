package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import javax.inject.Inject

class ValidateTakeProfitStopLossUseCase @Inject constructor() {
    operator fun invoke(
        direction: TradeDirection,
        entryPrice: Double,
        takeProfitStr: String,
        stopLossStr: String,
    ): ValidationResult {
        val tp = takeProfitStr.toDoubleOrNull()
        val sl = stopLossStr.toDoubleOrNull()

        if (takeProfitStr.isNotBlank()) {
            if (tp == null || tp <= 0.0) return ValidationResult.Invalid(ValidationError.INVALID_TAKE_PROFIT)
            when (direction) {
                TradeDirection.LONG -> if (tp <= entryPrice) return ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_ABOVE_ENTRY)
                TradeDirection.SHORT -> if (tp >= entryPrice) return ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_BELOW_ENTRY)
            }
        }

        if (stopLossStr.isNotBlank()) {
            if (sl == null || sl <= 0.0) return ValidationResult.Invalid(ValidationError.INVALID_STOP_LOSS)
            when (direction) {
                TradeDirection.LONG -> if (sl >= entryPrice) return ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_BELOW_ENTRY)
                TradeDirection.SHORT -> if (sl <= entryPrice) return ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_ABOVE_ENTRY)
            }
        }

        if (tp != null && sl != null && tp == sl) {
            return ValidationResult.Invalid(ValidationError.TAKE_PROFIT_EQUALS_STOP_LOSS)
        }

        return ValidationResult.Valid
    }
}
