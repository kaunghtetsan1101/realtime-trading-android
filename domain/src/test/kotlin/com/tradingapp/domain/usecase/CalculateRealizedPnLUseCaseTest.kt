package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateRealizedPnLUseCaseTest {

    private lateinit var useCase: CalculateRealizedPnLUseCase

    @Before
    fun setUp() {
        useCase = CalculateRealizedPnLUseCase()
    }

    @Test
    fun `LONG profit when close above entry`() {
        val pnl = useCase(TradeDirection.LONG, entryPrice = 50_000.0, closePrice = 55_000.0, quantity = 1.0)
        assertEquals(5_000.0, pnl, 0.001)
    }

    @Test
    fun `LONG loss when close below entry`() {
        val pnl = useCase(TradeDirection.LONG, entryPrice = 50_000.0, closePrice = 45_000.0, quantity = 1.0)
        assertEquals(-5_000.0, pnl, 0.001)
    }

    @Test
    fun `LONG zero pnl when close equals entry`() {
        val pnl = useCase(TradeDirection.LONG, entryPrice = 50_000.0, closePrice = 50_000.0, quantity = 1.0)
        assertEquals(0.0, pnl, 0.001)
    }

    @Test
    fun `LONG pnl scales with quantity`() {
        val pnl = useCase(TradeDirection.LONG, entryPrice = 50_000.0, closePrice = 51_000.0, quantity = 2.5)
        assertEquals(2_500.0, pnl, 0.001)
    }

    @Test
    fun `SHORT profit when close below entry`() {
        val pnl = useCase(TradeDirection.SHORT, entryPrice = 50_000.0, closePrice = 45_000.0, quantity = 1.0)
        assertEquals(5_000.0, pnl, 0.001)
    }

    @Test
    fun `SHORT loss when close above entry`() {
        val pnl = useCase(TradeDirection.SHORT, entryPrice = 50_000.0, closePrice = 55_000.0, quantity = 1.0)
        assertEquals(-5_000.0, pnl, 0.001)
    }

    @Test
    fun `SHORT zero pnl when close equals entry`() {
        val pnl = useCase(TradeDirection.SHORT, entryPrice = 50_000.0, closePrice = 50_000.0, quantity = 1.0)
        assertEquals(0.0, pnl, 0.001)
    }

    @Test
    fun `zero quantity produces zero pnl`() {
        val pnl = useCase(TradeDirection.LONG, entryPrice = 50_000.0, closePrice = 55_000.0, quantity = 0.0)
        assertEquals(0.0, pnl, 0.001)
    }
}
