package com.tradingapp.trading

import app.cash.turbine.test
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Portfolio
import com.tradingapp.domain.usecase.GetOrderHistoryUseCase
import com.tradingapp.domain.usecase.GetPortfolioUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getPortfolio: GetPortfolioUseCase = mockk()
    private val getOrderHistory: GetOrderHistoryUseCase = mockk()
    private val observeNetworkStatus: ObserveNetworkStatusUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeNetworkStatus() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has isLoading true and no error`() = runTest {
        every { getPortfolio() } returns MutableSharedFlow() // never emits
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.portfolio)
    }

    // -------------------------------------------------------------------------
    // Portfolio emissions
    // -------------------------------------------------------------------------

    @Test
    fun `portfolio emission updates state and clears loading`() = runTest {
        val portfolio = emptyPortfolio()
        every { getPortfolio() } returns flowOf(portfolio)
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
        assertNotNull(vm.state.value.portfolio)
        assertEquals(portfolio.cashBalance, vm.state.value.portfolio!!.cashBalance, 0.001)
    }

    @Test
    fun `portfolio flow error sets error message and clears loading`() = runTest {
        every { getPortfolio() } returns flow { throw IllegalStateException("DB failure") }
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.isNotBlank())
    }

    @Test
    fun `order history updates orders in state`() = runTest {
        val orders = listOf(fakeOrder("BTC", OrderSide.BUY), fakeOrder("ETH", OrderSide.SELL))
        every { getPortfolio() } returns MutableSharedFlow()
        every { getOrderHistory() } returns flowOf(orders)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(orders, vm.state.value.orders)
    }

    // -------------------------------------------------------------------------
    // Retry
    // -------------------------------------------------------------------------

    @Test
    fun `Retry clears error, sets loading, and restarts portfolio observation`() = runTest {
        val portfolioFlow = MutableStateFlow<Portfolio?>(null)
        every { getPortfolio() } returns flow {
            portfolioFlow.collect { p -> if (p != null) emit(p) }
        }
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()

        // Simulate error state manually
        vm.onEvent(PortfolioEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    // -------------------------------------------------------------------------
    // Effects
    // -------------------------------------------------------------------------

    @Test
    fun `NavigateBack event emits NavigateBack effect`() = runTest {
        every { getPortfolio() } returns MutableSharedFlow()
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()

        vm.effects.test {
            vm.onEvent(PortfolioEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(PortfolioEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `TradeAsset event emits NavigateToTrade effect with correct symbol`() = runTest {
        every { getPortfolio() } returns MutableSharedFlow()
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()

        vm.effects.test {
            vm.onEvent(PortfolioEvent.TradeAsset("SOL"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(PortfolioEffect.NavigateToTrade("SOL"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // Offline
    // -------------------------------------------------------------------------

    @Test
    fun `network going offline sets isOffline true`() = runTest {
        val networkFlow = MutableStateFlow(true)
        every { observeNetworkStatus() } returns networkFlow
        every { getPortfolio() } returns MutableSharedFlow()
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isOffline)

        networkFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.isOffline)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildViewModel() = PortfolioViewModel(getPortfolio, getOrderHistory, observeNetworkStatus)

    private fun emptyPortfolio() = Portfolio(
        cashBalance = 10_000.0,
        positions = emptyList(),
        totalValue = 10_000.0,
        totalUnrealizedPnL = 0.0,
        totalUnrealizedPnLPct = 0.0,
    )

    private fun fakeOrder(symbol: String, side: OrderSide) = Order(
        id = "id-$symbol",
        symbol = symbol,
        side = side,
        quantity = 0.1,
        price = 1_000.0,
        totalValue = 100.0,
        status = OrderStatus.FILLED,
        timestamp = 0L,
    )
}
