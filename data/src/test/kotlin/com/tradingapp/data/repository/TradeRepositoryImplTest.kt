package com.tradingapp.data.repository

import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.database.TradingDatabase
import com.tradingapp.database.dao.OrderDao
import com.tradingapp.database.dao.PositionDao
import com.tradingapp.database.dao.WalletDao
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.database.entity.WalletEntity
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TradeRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val db: TradingDatabase = mockk(relaxed = true)
    private val orderDao: OrderDao = mockk(relaxed = true)
    private val positionDao: PositionDao = mockk(relaxed = true)
    private val walletDao: WalletDao = mockk(relaxed = true)

    // Subclass bypasses Room's Android-specific withTransaction so tests run on JVM.
    private lateinit var repo: TradeRepositoryImpl

    @Before
    fun setUp() {
        repo = object : TradeRepositoryImpl(db, orderDao, positionDao, walletDao, dispatchers) {
            override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()
        }
    }

    // -------------------------------------------------------------------------
    // placeOrder — BUY path
    // -------------------------------------------------------------------------

    @Test
    fun `placeOrder BUY with no existing position creates new position at order price`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 10_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns null

        val positionSlot = slot<PositionEntity>()
        coEvery { positionDao.upsert(capture(positionSlot)) } returns Unit

        repo.placeOrder(buyOrder(qty = 0.5, price = 60_000.0))

        with(positionSlot.captured) {
            assertEquals("BTC", symbol)
            assertEquals(0.5, quantity, 0.001)
            assertEquals(60_000.0, avgPrice, 0.001)
        }
    }

    @Test
    fun `placeOrder BUY with existing position calculates weighted average price`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 10_000.0)
        // Existing: 1.0 BTC @ 50_000
        coEvery { positionDao.getBySymbol("BTC") } returns PositionEntity("BTC", 1.0, 50_000.0, 0L)

        val positionSlot = slot<PositionEntity>()
        coEvery { positionDao.upsert(capture(positionSlot)) } returns Unit

        // Buy 1.0 more BTC @ 60_000 → avg = (1×50_000 + 1×60_000) / 2 = 55_000
        repo.placeOrder(buyOrder(qty = 1.0, price = 60_000.0))

        assertEquals(2.0, positionSlot.captured.quantity, 0.001)
        assertEquals(55_000.0, positionSlot.captured.avgPrice, 0.001)
    }

    @Test
    fun `placeOrder BUY debits wallet by totalValue`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 10_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns null

        val walletSlot = slot<WalletEntity>()
        coEvery { walletDao.upsert(capture(walletSlot)) } returns Unit

        // Buy 0.1 BTC @ 60_000 → debit 6_000 → new balance 4_000
        repo.placeOrder(buyOrder(qty = 0.1, price = 60_000.0))

        assertEquals(4_000.0, walletSlot.captured.cashBalance, 0.001)
    }

    @Test
    fun `placeOrder BUY with null wallet uses INITIAL_BALANCE`() = runTest {
        coEvery { walletDao.get() } returns null
        coEvery { positionDao.getBySymbol("BTC") } returns null

        val walletSlot = slot<WalletEntity>()
        coEvery { walletDao.upsert(capture(walletSlot)) } returns Unit

        repo.placeOrder(buyOrder(qty = 0.1, price = 1_000.0))

        // INITIAL_BALANCE (10_000) − 0.1 × 1_000 = 9_900
        assertEquals(9_900.0, walletSlot.captured.cashBalance, 0.001)
    }

    // -------------------------------------------------------------------------
    // placeOrder — SELL path
    // -------------------------------------------------------------------------

    @Test
    fun `placeOrder SELL partial reduces position quantity and preserves average price`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 1_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns PositionEntity("BTC", 2.0, 50_000.0, 0L)

        val positionSlot = slot<PositionEntity>()
        coEvery { positionDao.upsert(capture(positionSlot)) } returns Unit

        repo.placeOrder(sellOrder(qty = 0.5, price = 60_000.0))

        assertEquals(1.5, positionSlot.captured.quantity, 0.001)
        assertEquals(50_000.0, positionSlot.captured.avgPrice, 0.001) // avg price unchanged on SELL
    }

    @Test
    fun `placeOrder SELL all quantity deletes position`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 1_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns PositionEntity("BTC", 1.0, 50_000.0, 0L)

        repo.placeOrder(sellOrder(qty = 1.0, price = 60_000.0))

        coVerify { positionDao.deleteBySymbol("BTC") }
        coVerify(exactly = 0) { positionDao.upsert(any()) }
    }

    @Test
    fun `placeOrder SELL credits wallet by totalValue`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 1_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns PositionEntity("BTC", 1.0, 50_000.0, 0L)

        val walletSlot = slot<WalletEntity>()
        coEvery { walletDao.upsert(capture(walletSlot)) } returns Unit

        // Sell 0.5 BTC @ 60_000 → credit 30_000 → new balance 31_000
        repo.placeOrder(sellOrder(qty = 0.5, price = 60_000.0))

        assertEquals(31_000.0, walletSlot.captured.cashBalance, 0.001)
    }

    // -------------------------------------------------------------------------
    // placeOrder — result and error
    // -------------------------------------------------------------------------

    @Test
    fun `placeOrder inserts order entity and returns the order on success`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 10_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns null

        val order = buyOrder(qty = 0.1, price = 60_000.0)
        val result = repo.placeOrder(order)

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify { orderDao.insert(any()) }
    }

    @Test
    fun `placeOrder propagates DAO failure as Result failure`() = runTest {
        coEvery { walletDao.get() } returns WalletEntity(cashBalance = 10_000.0)
        coEvery { positionDao.getBySymbol("BTC") } returns null
        coEvery { orderDao.insert(any()) } throws RuntimeException("DB write failed")

        val result = repo.placeOrder(buyOrder())

        assertTrue(result.isFailure)
        assertEquals("DB write failed", result.exceptionOrNull()?.message)
    }

    // -------------------------------------------------------------------------
    // observeCashBalance
    // -------------------------------------------------------------------------

    @Test
    fun `observeCashBalance returns INITIAL_BALANCE when wallet row is absent`() = runTest {
        every { walletDao.observe() } returns flowOf(null)

        val balance = repo.observeCashBalance().first()

        assertEquals(WalletEntity.INITIAL_BALANCE, balance, 0.001)
    }

    @Test
    fun `observeCashBalance emits the stored balance`() = runTest {
        every { walletDao.observe() } returns flowOf(WalletEntity(cashBalance = 7_500.0))

        val balance = repo.observeCashBalance().first()

        assertEquals(7_500.0, balance, 0.001)
    }

    // -------------------------------------------------------------------------
    // observePositions / observeOrders
    // -------------------------------------------------------------------------

    @Test
    fun `observePositions maps PositionEntity to domain Position`() = runTest {
        every { positionDao.observeAll() } returns flowOf(
            listOf(PositionEntity("ETH", 2.5, 3_000.0, 0L)),
        )

        val positions = repo.observePositions().first()

        assertEquals(1, positions.size)
        with(positions.first()) {
            assertEquals("ETH", symbol)
            assertEquals(2.5, quantity, 0.001)
            assertEquals(3_000.0, averagePrice, 0.001)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buyOrder(qty: Double = 0.1, price: Double = 60_000.0) = Order(
        id = "test-id",
        symbol = "BTC",
        side = OrderSide.BUY,
        quantity = qty,
        price = price,
        totalValue = qty * price,
        status = OrderStatus.FILLED,
        timestamp = 0L,
    )

    private fun sellOrder(qty: Double = 0.5, price: Double = 60_000.0) = Order(
        id = "test-sell",
        symbol = "BTC",
        side = OrderSide.SELL,
        quantity = qty,
        price = price,
        totalValue = qty * price,
        status = OrderStatus.FILLED,
        timestamp = 0L,
    )
}
