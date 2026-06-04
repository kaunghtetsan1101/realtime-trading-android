package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import javax.inject.Inject

class ValidateOrderUseCase @Inject constructor() {
    operator fun invoke(
        side: OrderSide,
        quantityStr: String,
        currentPrice: Double,
        cashBalance: Double,
        existingPosition: Position?,
    ): ValidationResult {
        if (quantityStr.isBlank()) return ValidationResult.Invalid(ValidationError.EMPTY_QUANTITY)
        val quantity = quantityStr.toDoubleOrNull()
            ?: return ValidationResult.Invalid(ValidationError.INVALID_QUANTITY)
        if (quantity <= 0.0) return ValidationResult.Invalid(ValidationError.ZERO_QUANTITY)
        return when (side) {
            OrderSide.BUY -> {
                val total = quantity * currentPrice
                if (total > cashBalance) {
                    ValidationResult.Invalid(ValidationError.INSUFFICIENT_BALANCE)
                } else {
                    ValidationResult.Valid
                }
            }
            OrderSide.SELL -> {
                val held = existingPosition?.quantity ?: 0.0
                if (quantity > held) {
                    ValidationResult.Invalid(ValidationError.INSUFFICIENT_POSITION)
                } else {
                    ValidationResult.Valid
                }
            }
        }
    }
}
