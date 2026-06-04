package com.tradingapp.trading

import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.usecase.ValidateOrderUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValidateOrderUseCaseTest {

    private lateinit var useCase: ValidateOrderUseCase

    @Before
    fun setUp() {
        useCase = ValidateOrderUseCase()
    }

    // --- BUY validations ---

    @Test
    fun `BUY with blank quantity returns EMPTY_QUANTITY`() {
        val result = useCase(OrderSide.BUY, "   ", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.EMPTY_QUANTITY), result)
    }

    @Test
    fun `BUY with non-numeric quantity returns INVALID_QUANTITY`() {
        val result = useCase(OrderSide.BUY, "abc", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.INVALID_QUANTITY), result)
    }

    @Test
    fun `BUY with zero quantity returns ZERO_QUANTITY`() {
        val result = useCase(OrderSide.BUY, "0", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.ZERO_QUANTITY), result)
    }

    @Test
    fun `BUY with negative quantity returns ZERO_QUANTITY`() {
        val result = useCase(OrderSide.BUY, "-1", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.ZERO_QUANTITY), result)
    }

    @Test
    fun `BUY total exceeds cash balance returns INSUFFICIENT_BALANCE`() {
        // 10 * $150 = $1500 but only $1000 available
        val result = useCase(OrderSide.BUY, "10", 150.0, 1_000.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.INSUFFICIENT_BALANCE), result)
    }

    @Test
    fun `BUY total equals cash balance is Valid`() {
        val result = useCase(OrderSide.BUY, "10", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `BUY total under cash balance is Valid`() {
        val result = useCase(OrderSide.BUY, "5", 100.0, 1_000.0, null)
        assertEquals(ValidationResult.Valid, result)
    }

    // --- SELL validations ---

    @Test
    fun `SELL with no position returns INSUFFICIENT_POSITION`() {
        val result = useCase(OrderSide.SELL, "1.0", 100.0, 0.0, null)
        assertEquals(ValidationResult.Invalid(ValidationError.INSUFFICIENT_POSITION), result)
    }

    @Test
    fun `SELL more than held position returns INSUFFICIENT_POSITION`() {
        val position = Position("BTC", quantity = 0.5, averagePrice = 60_000.0)
        val result = useCase(OrderSide.SELL, "1.0", 60_000.0, 0.0, position)
        assertEquals(ValidationResult.Invalid(ValidationError.INSUFFICIENT_POSITION), result)
    }

    @Test
    fun `SELL exact held amount is Valid`() {
        val position = Position("BTC", quantity = 1.0, averagePrice = 60_000.0)
        val result = useCase(OrderSide.SELL, "1.0", 60_000.0, 0.0, position)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SELL partial amount is Valid`() {
        val position = Position("BTC", quantity = 2.0, averagePrice = 60_000.0)
        val result = useCase(OrderSide.SELL, "0.5", 60_000.0, 0.0, position)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SELL with blank quantity returns EMPTY_QUANTITY`() {
        val position = Position("BTC", quantity = 1.0, averagePrice = 60_000.0)
        val result = useCase(OrderSide.SELL, "", 60_000.0, 0.0, position)
        assertEquals(ValidationResult.Invalid(ValidationError.EMPTY_QUANTITY), result)
    }
}
