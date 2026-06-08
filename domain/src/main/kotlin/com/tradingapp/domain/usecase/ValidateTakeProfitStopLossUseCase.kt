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
        validateTakeProfit(direction, entryPrice, takeProfitStr)?.let { return it }
        validateStopLoss(direction, entryPrice, stopLossStr)?.let { return it }
        return validateNotEqual(takeProfitStr, stopLossStr)
    }

    private fun validateTakeProfit(
        direction: TradeDirection,
        entryPrice: Double,
        takeProfitStr: String,
    ): ValidationResult.Invalid? {
        if (takeProfitStr.isBlank()) return null
        val tp = takeProfitStr.toDoubleOrNull()
        if (tp == null || tp <= 0.0) {
            return ValidationResult.Invalid(ValidationError.INVALID_TAKE_PROFIT)
        }
        return when (direction) {
            TradeDirection.LONG -> if (tp <= entryPrice) {
                ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_ABOVE_ENTRY)
            } else {
                null
            }
            TradeDirection.SHORT -> if (tp >= entryPrice) {
                ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_BELOW_ENTRY)
            } else {
                null
            }
        }
    }

    private fun validateStopLoss(
        direction: TradeDirection,
        entryPrice: Double,
        stopLossStr: String,
    ): ValidationResult.Invalid? {
        if (stopLossStr.isBlank()) return null
        val sl = stopLossStr.toDoubleOrNull()
        if (sl == null || sl <= 0.0) {
            return ValidationResult.Invalid(ValidationError.INVALID_STOP_LOSS)
        }
        return when (direction) {
            TradeDirection.LONG -> if (sl >= entryPrice) {
                ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_BELOW_ENTRY)
            } else {
                null
            }
            TradeDirection.SHORT -> if (sl <= entryPrice) {
                ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_ABOVE_ENTRY)
            } else {
                null
            }
        }
    }

    private fun validateNotEqual(takeProfitStr: String, stopLossStr: String): ValidationResult {
        val tp = takeProfitStr.toDoubleOrNull()
        val sl = stopLossStr.toDoubleOrNull()
        return if (tp != null && sl != null && tp == sl) {
            ValidationResult.Invalid(ValidationError.TAKE_PROFIT_EQUALS_STOP_LOSS)
        } else {
            ValidationResult.Valid
        }
    }
}
