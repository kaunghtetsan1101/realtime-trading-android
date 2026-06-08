package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValidateTakeProfitStopLossUseCaseTest {

    private lateinit var useCase: ValidateTakeProfitStopLossUseCase

    @Before
    fun setUp() {
        useCase = ValidateTakeProfitStopLossUseCase()
    }

    // -------------------------------------------------------------------------
    // Long position — Take Profit
    // -------------------------------------------------------------------------

    @Test
    fun `LONG valid TP above entry passes`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "55000", "")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `LONG TP equal to entry fails`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "50000", "")
        assertEquals(ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_ABOVE_ENTRY), result)
    }

    @Test
    fun `LONG TP below entry fails`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "45000", "")
        assertEquals(ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_ABOVE_ENTRY), result)
    }

    // -------------------------------------------------------------------------
    // Long position — Stop Loss
    // -------------------------------------------------------------------------

    @Test
    fun `LONG valid SL below entry passes`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "45000")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `LONG SL equal to entry fails`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "50000")
        assertEquals(ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_BELOW_ENTRY), result)
    }

    @Test
    fun `LONG SL above entry fails`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "55000")
        assertEquals(ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_BELOW_ENTRY), result)
    }

    // -------------------------------------------------------------------------
    // Short position — Take Profit
    // -------------------------------------------------------------------------

    @Test
    fun `SHORT valid TP below entry passes`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "45000", "")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SHORT TP equal to entry fails`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "50000", "")
        assertEquals(ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_BELOW_ENTRY), result)
    }

    @Test
    fun `SHORT TP above entry fails`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "55000", "")
        assertEquals(ValidationResult.Invalid(ValidationError.TAKE_PROFIT_MUST_BE_BELOW_ENTRY), result)
    }

    // -------------------------------------------------------------------------
    // Short position — Stop Loss
    // -------------------------------------------------------------------------

    @Test
    fun `SHORT valid SL above entry passes`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "", "55000")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SHORT SL equal to entry fails`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "", "50000")
        assertEquals(ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_ABOVE_ENTRY), result)
    }

    @Test
    fun `SHORT SL below entry fails`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "", "45000")
        assertEquals(ValidationResult.Invalid(ValidationError.STOP_LOSS_MUST_BE_ABOVE_ENTRY), result)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `both blank passes (TP and SL are optional)`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `TP zero is invalid`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "0", "")
        assertEquals(ValidationResult.Invalid(ValidationError.INVALID_TAKE_PROFIT), result)
    }

    @Test
    fun `SL zero is invalid`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "0")
        assertEquals(ValidationResult.Invalid(ValidationError.INVALID_STOP_LOSS), result)
    }

    @Test
    fun `TP and SL equal returns TAKE_PROFIT_EQUALS_STOP_LOSS`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "55000", "55000")
        assertEquals(ValidationResult.Invalid(ValidationError.TAKE_PROFIT_EQUALS_STOP_LOSS), result)
    }

    @Test
    fun `non-numeric TP is invalid`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "abc", "")
        assertEquals(ValidationResult.Invalid(ValidationError.INVALID_TAKE_PROFIT), result)
    }

    @Test
    fun `non-numeric SL is invalid`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "", "abc")
        assertEquals(ValidationResult.Invalid(ValidationError.INVALID_STOP_LOSS), result)
    }

    @Test
    fun `LONG both valid TP and SL passes`() {
        val result = useCase(TradeDirection.LONG, 50_000.0, "55000", "45000")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SHORT both valid TP and SL passes`() {
        val result = useCase(TradeDirection.SHORT, 50_000.0, "45000", "55000")
        assertEquals(ValidationResult.Valid, result)
    }
}
