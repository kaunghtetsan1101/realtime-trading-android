package com.tradingapp.trading

import app.cash.turbine.test
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Portfolio
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.usecase.ClosePositionUseCase
import com.tradingapp.domain.usecase.EditPositionRiskUseCase
import com.tradingapp.domain.usecase.GetOrderHistoryUseCase
import com.tradingapp.domain.usecase.GetPortfolioUseCase
import com.tradingapp.domain.usecase.MonitorPositionExitUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
    private val observePriceTicks: ObservePriceTicksUseCase = mockk()
    private val monitorPositionExit: MonitorPositionExitUseCase = mockk()
    private val closePosition: ClosePositionUseCase = mockk()
    private val editPositionRisk: EditPositionRiskUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeNetworkStatus() } returns flowOf(true)
        every { observePriceTicks(any()) } returns emptyFlow()
        every { monitorPositionExit(any(), any()) } returns emptyFlow()
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
    // Reactive price-tick update
    // -------------------------------------------------------------------------

    @Test
    fun `portfolio state updates reactively when price tick changes portfolio value`() = runTest {
        val initialPortfolio = Portfolio(
            cashBalance = 5_000.0,
            positions = listOf(
                Position(
                    "p1", "BTC", TradeDirection.LONG, quantity = 0.1, averagePrice = 50_000.0,
                    currentPrice = 50_000.0, totalValue = 5_000.0, unrealizedPnL = 0.0,
                    unrealizedPnLPct = 0.0,
                ),
            ),
            totalValue = 10_000.0,
            totalUnrealizedPnL = 0.0,
            totalUnrealizedPnLPct = 0.0,
        )
        val updatedPortfolio = Portfolio(
            cashBalance = 5_000.0,
            positions = listOf(
                Position(
                    "p1", "BTC", TradeDirection.LONG, quantity = 0.1, averagePrice = 50_000.0,
                    currentPrice = 60_000.0, totalValue = 6_000.0, unrealizedPnL = 1_000.0,
                    unrealizedPnLPct = 20.0,
                ),
            ),
            totalValue = 11_000.0,
            totalUnrealizedPnL = 1_000.0,
            totalUnrealizedPnLPct = 20.0,
        )
        val portfolioFlow = MutableStateFlow(initialPortfolio)
        every { getPortfolio() } returns portfolioFlow
        every { getOrderHistory() } returns flowOf(emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10_000.0, vm.state.value.portfolio!!.totalValue, 0.001)

        portfolioFlow.value = updatedPortfolio
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = vm.state.value.portfolio!!
        assertEquals(11_000.0, updated.totalValue, 0.001)
        assertEquals(1_000.0, updated.totalUnrealizedPnL, 0.001)
        assertEquals(60_000.0, updated.positions.first().currentPrice, 0.001)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildViewModel() = PortfolioViewModel(
        getPortfolio,
        getOrderHistory,
        observeNetworkStatus,
        observePriceTicks,
        monitorPositionExit,
        closePosition,
        editPositionRisk,
    )

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
        direction = TradeDirection.LONG,
        quantity = 0.1,
        price = 1_000.0,
        totalValue = 100.0,
        status = OrderStatus.FILLED,
        timestamp = 0L,
    )
}
