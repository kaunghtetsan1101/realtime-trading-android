package com.tradingapp.trading

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.repository.TradeRepository
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import com.tradingapp.domain.usecase.PlaceOrderUseCase
import com.tradingapp.domain.usecase.ValidateOrderUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TradingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAssetDetail: GetAssetDetailUseCase = mockk()
    private val observePriceTicks: ObservePriceTicksUseCase = mockk()
    private val validateOrder: ValidateOrderUseCase = mockk()
    private val placeOrder: PlaceOrderUseCase = mockk()
    private val tradeRepository: TradeRepository = mockk()

    private fun buildViewModel(): TradingViewModel {
        every { getAssetDetail(any()) } returns flowOf(Result.Loading)
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        return TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has BUY side and empty quantity`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(OrderSide.BUY, state.selectedSide)
        assertEquals("", state.quantityInput)
        assertNull(state.validationError)
    }

    @Test
    fun `SideSelected event switches side`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.SideSelected(OrderSide.SELL))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OrderSide.SELL, vm.state.value.selectedSide)
    }

    @Test
    fun `QuantityChanged filters non-numeric characters`() = runTest {
        val vm = buildViewModel()
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged("1a2b.3c4"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("12.34", vm.state.value.quantityInput)
    }

    @Test
    fun `QuantityChanged propagates INSUFFICIENT_BALANCE error`() = runTest {
        every { getAssetDetail(any()) } returns flowOf(
            Result.Success(fakeAsset(price = 70_000.0)),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(1_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observePriceTicks(any()) } returns emptyFlow()

        every { validateOrder(any(), any(), any(), any(), any()) } returns
            ValidationResult.Invalid(ValidationError.INSUFFICIENT_BALANCE)

        val vm = TradingViewModel("BTC", getAssetDetail, observePriceTicks, validateOrder, placeOrder, tradeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged("1.0"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ValidationError.INSUFFICIENT_BALANCE, vm.state.value.validationError)
    }

    @Test
    fun `ReviewOrder event shows confirmation sheet`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.ReviewOrder)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.isReviewVisible)
    }

    @Test
    fun `DismissReview hides confirmation sheet`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.ReviewOrder)
        vm.onEvent(TradingEvent.DismissReview)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!vm.state.value.isReviewVisible)
    }

    @Test
    fun `ConfirmOrder success sends ShowSnackbar effect and hides sheet`() = runTest {
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 60_000.0)))
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid

        val fakeOrder = Order("1", "BTC", OrderSide.BUY, 0.1, 60_000.0, 6_000.0, OrderStatus.FILLED, 0L)
        coEvery { placeOrder(any(), any(), any(), any()) } returns kotlin.Result.success(fakeOrder)

        val vm = TradingViewModel("BTC", getAssetDetail, observePriceTicks, validateOrder, placeOrder, tradeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged("0.1"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.effects.test {
            vm.onEvent(TradingEvent.ConfirmOrder)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TradingEffect.ShowSnackbar)
            assertTrue((effect as TradingEffect.ShowSnackbar).message.contains("Order placed"))
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(!vm.state.value.isReviewVisible)
        assertTrue(!vm.state.value.isPlacingOrder)
    }

    @Test
    fun `NavigateBack event emits NavigateBack effect`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.effects.test {
            vm.onEvent(TradingEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(TradingEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helpers ---

    private fun fakeAsset(price: Double) = Asset(
        symbol = "BTC",
        name = "Bitcoin",
        currentPrice = price,
        priceChange24h = 0.0,
        priceChangePct24h = 0.0,
        marketCap = 0.0,
        volume24h = 0.0,
        logoUrl = null,
        isFavorite = false,
        lastUpdated = 0L,
    )
}
