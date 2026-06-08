package com.tradingapp.trading

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.repository.TradeRepository
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
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
import org.junit.Assert.assertFalse
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
    private val validateTpSl: com.tradingapp.domain.usecase.ValidateTakeProfitStopLossUseCase = mockk()
    private val placeOrder: PlaceOrderUseCase = mockk()
    private val tradeRepository: TradeRepository = mockk()
    private val observeNetworkStatus: ObserveNetworkStatusUseCase = mockk()

    private fun buildViewModel(assetPrice: Double = 0.0, cashBalance: Double = 10_000.0): TradingViewModel {
        every { getAssetDetail(any()) } returns flowOf(
            if (assetPrice > 0) Result.Success(fakeAsset(price = assetPrice)) else Result.Loading,
        )
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(cashBalance)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observeNetworkStatus() } returns flowOf(true)
        return TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { validateTpSl(any(), any(), any(), any()) } returns ValidationResult.Valid
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
        every { observeNetworkStatus() } returns flowOf(true)

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
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
        every { observeNetworkStatus() } returns flowOf(true)

        val fakeOrder = Order("1", "BTC", OrderSide.BUY, com.tradingapp.domain.model.TradeDirection.LONG, 0.1, 60_000.0, 6_000.0, OrderStatus.FILLED, 0L)
        coEvery { placeOrder(any(), any(), any(), any(), any(), any()) } returns kotlin.Result.success(fakeOrder)

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
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
    fun `ConfirmOrder failure sends ShowSnackbar with error message and hides sheet`() = runTest {
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 60_000.0)))
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid
        every { observeNetworkStatus() } returns flowOf(true)
        coEvery { placeOrder(any(), any(), any(), any(), any(), any()) } returns
            kotlin.Result.failure(RuntimeException("Insufficient funds"))

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged("0.1"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.effects.test {
            vm.onEvent(TradingEvent.ConfirmOrder)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TradingEffect.ShowSnackbar)
            assertTrue((effect as TradingEffect.ShowSnackbar).message.contains("Order failed"))
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(vm.state.value.isReviewVisible)
        assertFalse(vm.state.value.isPlacingOrder)
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

    @Test
    fun `Retry event reloads asset detail`() = runTest {
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 60_000.0)))
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observeNetworkStatus() } returns flowOf(true)

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(60_000.0, vm.state.value.currentPrice, 0.001)
    }

    @Test
    fun `price tick from WebSocket updates currentPrice in state`() = runTest {
        val tickFlow = kotlinx.coroutines.flow.MutableSharedFlow<PriceTick>(extraBufferCapacity = 1)
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 60_000.0)))
        every { observePriceTicks(any()) } returns tickFlow
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observeNetworkStatus() } returns flowOf(true)
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        tickFlow.emit(PriceTick("BTC", 65_000.0, 0L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(65_000.0, vm.state.value.currentPrice, 0.001)
    }

    @Test
    fun `QuickFillSelected 50 percent fills half of cash balance for BUY`() = runTest {
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 40_000.0)))
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observeNetworkStatus() } returns flowOf(true)
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuickFillSelected(0.5))
        testDispatcher.scheduler.advanceUntilIdle()

        // 50% of 10_000 cash / 40_000 price = 0.125 BTC
        assertEquals("0.125", vm.state.value.quantityInput)
    }

    @Test
    fun `QuickFillSelected MAX fills all available position for SELL`() = runTest {
        val position = com.tradingapp.domain.model.Position("p1", "BTC", com.tradingapp.domain.model.TradeDirection.LONG, 2.0, 50_000.0)
        every { getAssetDetail(any()) } returns flowOf(Result.Success(fakeAsset(price = 60_000.0)))
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(1_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(position)
        every { observeNetworkStatus() } returns flowOf(true)
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.SideSelected(OrderSide.SELL))
        vm.onEvent(TradingEvent.QuickFillSelected(1.0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2", vm.state.value.quantityInput) // all 2.0 BTC
    }

    @Test
    fun `QuantityChanged with leading dot is kept as-is`() = runTest {
        val vm = buildViewModel()
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged(".5"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(".5", vm.state.value.quantityInput)
    }

    @Test
    fun `QuantityChanged strips second decimal point`() = runTest {
        val vm = buildViewModel()
        every { validateOrder(any(), any(), any(), any(), any()) } returns ValidationResult.Valid
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(TradingEvent.QuantityChanged("1.2.3"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("1.23", vm.state.value.quantityInput)
    }

    @Test
    fun `network going offline sets isOffline in TradingState`() = runTest {
        val networkFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        every { getAssetDetail(any()) } returns flowOf(Result.Loading)
        every { observePriceTicks(any()) } returns emptyFlow()
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)
        every { tradeRepository.observePosition(any()) } returns flowOf(null)
        every { observeNetworkStatus() } returns networkFlow

        val vm = TradingViewModel(
            symbol = "BTC",
            getAssetDetail = getAssetDetail,
            observePriceTicks = observePriceTicks,
            validateOrder = validateOrder,
            validateTpSl = validateTpSl,
            placeOrder = placeOrder,
            tradeRepository = tradeRepository,
            observeNetworkStatus = observeNetworkStatus,
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isOffline)

        networkFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.isOffline)
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
