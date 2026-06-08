package com.tradingapp.trading

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.repository.TradeRepository
import com.tradingapp.domain.usecase.PlaceOrderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaceOrderUseCaseTest {

    private val tradeRepository: TradeRepository = mockk()
    private lateinit var useCase: PlaceOrderUseCase

    @Before
    fun setUp() {
        useCase = PlaceOrderUseCase(tradeRepository)
    }

    @Test
    fun `invoke creates correct Order and delegates to repository`() = runTest {
        val orderSlot = slot<Order>()
        coEvery { tradeRepository.placeOrder(capture(orderSlot), any(), any()) } answers {
            Result.success(orderSlot.captured)
        }

        val result = useCase(
            side = OrderSide.BUY,
            symbol = "BTC",
            quantity = 0.5,
            executionPrice = 60_000.0,
        )

        assertTrue(result.isSuccess)
        with(orderSlot.captured) {
            assertEquals("BTC", symbol)
            assertEquals(OrderSide.BUY, side)
            assertEquals(com.tradingapp.domain.model.TradeDirection.LONG, direction)
            assertEquals(0.5, quantity, 0.0)
            assertEquals(60_000.0, price, 0.0)
            assertEquals(30_000.0, totalValue, 0.0)
            assertEquals(OrderStatus.FILLED, status)
        }
        coVerify(exactly = 1) { tradeRepository.placeOrder(any(), isNull(), isNull()) }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = RuntimeException("DB error")
        coEvery { tradeRepository.placeOrder(any(), any(), any()) } returns Result.failure(error)

        val result = useCase(OrderSide.BUY, "BTC", 1.0, 60_000.0)

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke generates unique IDs for successive orders`() = runTest {
        val ids = mutableListOf<String>()
        coEvery { tradeRepository.placeOrder(any(), any(), any()) } answers {
            val order = firstArg<Order>()
            ids.add(order.id)
            Result.success(order)
        }

        useCase(OrderSide.BUY, "BTC", 0.1, 60_000.0)
        useCase(OrderSide.BUY, "ETH", 1.0, 3_000.0)

        assertEquals(2, ids.size)
        assertTrue(ids[0] != ids[1])
    }

    @Test
    fun `invoke SELL order sets correct side and totalValue`() = runTest {
        val orderSlot = slot<Order>()
        coEvery { tradeRepository.placeOrder(capture(orderSlot), any(), any()) } answers {
            Result.success(orderSlot.captured)
        }

        useCase(OrderSide.SELL, "ETH", 1.5, 3_200.0)

        with(orderSlot.captured) {
            assertEquals(OrderSide.SELL, side)
            assertEquals(com.tradingapp.domain.model.TradeDirection.SHORT, direction)
            assertEquals("ETH", symbol)
            assertEquals(1.5, quantity, 0.0)
            assertEquals(3_200.0, price, 0.0)
            assertEquals(4_800.0, totalValue, 0.001) // 1.5 × 3_200
            assertEquals(OrderStatus.FILLED, status)
        }
    }

    @Test
    fun `invoke sets status to FILLED regardless of side`() = runTest {
        coEvery { tradeRepository.placeOrder(any(), any(), any()) } answers { Result.success(firstArg()) }

        val buyResult = useCase(OrderSide.BUY, "BTC", 0.1, 60_000.0)
        val sellResult = useCase(OrderSide.SELL, "BTC", 0.1, 60_000.0)

        assertEquals(OrderStatus.FILLED, buyResult.getOrThrow().status)
        assertEquals(OrderStatus.FILLED, sellResult.getOrThrow().status)
    }

    @Test
    fun `invoke totalValue equals quantity times price`() = runTest {
        val orderSlot = slot<Order>()
        coEvery { tradeRepository.placeOrder(capture(orderSlot), any(), any()) } answers { Result.success(firstArg()) }

        useCase(OrderSide.BUY, "BTC", 0.25, 48_000.0)

        assertEquals(12_000.0, orderSlot.captured.totalValue, 0.001) // 0.25 × 48_000
    }

    @Test
    fun `invoke wraps repository exception in Result failure`() = runTest {
        coEvery { tradeRepository.placeOrder(any(), any(), any()) } throws RuntimeException("Unexpected")

        val result = useCase(OrderSide.BUY, "BTC", 0.1, 60_000.0)

        assertTrue(result.isFailure)
        assertEquals("Unexpected", result.exceptionOrNull()?.message)
    }
}
