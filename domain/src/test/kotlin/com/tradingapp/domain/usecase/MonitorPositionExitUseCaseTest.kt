package com.tradingapp.domain.usecase

import app.cash.turbine.test
import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.TradeDirection
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MonitorPositionExitUseCaseTest {

    private lateinit var useCase: MonitorPositionExitUseCase

    @Before
    fun setUp() {
        useCase = MonitorPositionExitUseCase()
    }

    // -------------------------------------------------------------------------
    // Long positions
    // -------------------------------------------------------------------------

    @Test
    fun `LONG emits TAKE_PROFIT_TRIGGERED when price reaches TP`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = 55_000.0, sl = 45_000.0)
        val priceFlow = flowOf(51_000.0, 53_000.0, 55_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.TAKE_PROFIT_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `LONG emits STOP_LOSS_TRIGGERED when price drops to SL`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = 55_000.0, sl = 45_000.0)
        val priceFlow = flowOf(49_000.0, 47_000.0, 45_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.STOP_LOSS_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `LONG emits nothing when price stays between SL and TP`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = 55_000.0, sl = 45_000.0)
        val priceFlow = flowOf(50_000.0, 51_000.0, 52_000.0)

        useCase(position, priceFlow).test {
            awaitComplete()
        }
    }

    @Test
    fun `LONG with no TP and no SL emits nothing`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = null, sl = null)
        val priceFlow = flowOf(10_000.0, 100_000.0)

        useCase(position, priceFlow).test {
            awaitComplete()
        }
    }

    @Test
    fun `LONG with only TP set fires on TP hit`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = 55_000.0, sl = null)
        val priceFlow = flowOf(55_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.TAKE_PROFIT_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `LONG with only SL set fires on SL hit`() = runTest {
        val position = longPosition(entryPrice = 50_000.0, tp = null, sl = 45_000.0)
        val priceFlow = flowOf(45_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.STOP_LOSS_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    // -------------------------------------------------------------------------
    // Short positions
    // -------------------------------------------------------------------------

    @Test
    fun `SHORT emits TAKE_PROFIT_TRIGGERED when price drops to TP`() = runTest {
        val position = shortPosition(entryPrice = 50_000.0, tp = 45_000.0, sl = 55_000.0)
        val priceFlow = flowOf(48_000.0, 46_000.0, 45_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.TAKE_PROFIT_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `SHORT emits STOP_LOSS_TRIGGERED when price rises to SL`() = runTest {
        val position = shortPosition(entryPrice = 50_000.0, tp = 45_000.0, sl = 55_000.0)
        val priceFlow = flowOf(52_000.0, 54_000.0, 55_000.0)

        useCase(position, priceFlow).test {
            assertEquals(CloseReason.STOP_LOSS_TRIGGERED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `SHORT emits nothing when price stays between TP and SL`() = runTest {
        val position = shortPosition(entryPrice = 50_000.0, tp = 45_000.0, sl = 55_000.0)
        val priceFlow = flowOf(50_000.0, 49_000.0, 51_000.0)

        useCase(position, priceFlow).test {
            awaitComplete()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun longPosition(entryPrice: Double, tp: Double?, sl: Double?) = Position(
        id = "pos-1",
        symbol = "BTC",
        direction = TradeDirection.LONG,
        quantity = 1.0,
        averagePrice = entryPrice,
        takeProfit = tp,
        stopLoss = sl,
    )

    private fun shortPosition(entryPrice: Double, tp: Double?, sl: Double?) = Position(
        id = "pos-2",
        symbol = "ETH",
        direction = TradeDirection.SHORT,
        quantity = 1.0,
        averagePrice = entryPrice,
        takeProfit = tp,
        stopLoss = sl,
    )
}
