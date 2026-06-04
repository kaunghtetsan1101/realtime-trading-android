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
        coEvery { tradeRepository.placeOrder(capture(orderSlot)) } answers {
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
            assertEquals(0.5, quantity, 0.0)
            assertEquals(60_000.0, price, 0.0)
            assertEquals(30_000.0, totalValue, 0.0)
            assertEquals(OrderStatus.FILLED, status)
        }
        coVerify(exactly = 1) { tradeRepository.placeOrder(any()) }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = RuntimeException("DB error")
        coEvery { tradeRepository.placeOrder(any()) } returns Result.failure(error)

        val result = useCase(OrderSide.BUY, "BTC", 1.0, 60_000.0)

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke generates unique IDs for successive orders`() = runTest {
        val ids = mutableListOf<String>()
        coEvery { tradeRepository.placeOrder(any()) } answers {
            val order = firstArg<Order>()
            ids.add(order.id)
            Result.success(order)
        }

        useCase(OrderSide.BUY, "BTC", 0.1, 60_000.0)
        useCase(OrderSide.BUY, "ETH", 1.0, 3_000.0)

        assertEquals(2, ids.size)
        assertTrue(ids[0] != ids[1])
    }
}
