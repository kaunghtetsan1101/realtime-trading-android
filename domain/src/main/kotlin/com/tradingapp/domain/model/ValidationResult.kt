package com.tradingapp.domain.model

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val error: ValidationError) : ValidationResult
}

enum class ValidationError {
    EMPTY_QUANTITY,
    INVALID_QUANTITY,
    ZERO_QUANTITY,
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_POSITION,
    INVALID_TAKE_PROFIT,
    INVALID_STOP_LOSS,
    TAKE_PROFIT_MUST_BE_ABOVE_ENTRY,
    TAKE_PROFIT_MUST_BE_BELOW_ENTRY,
    STOP_LOSS_MUST_BE_BELOW_ENTRY,
    STOP_LOSS_MUST_BE_ABOVE_ENTRY,
    TAKE_PROFIT_EQUALS_STOP_LOSS,
}
