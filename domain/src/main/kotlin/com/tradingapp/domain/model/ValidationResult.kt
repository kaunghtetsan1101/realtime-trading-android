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
}
