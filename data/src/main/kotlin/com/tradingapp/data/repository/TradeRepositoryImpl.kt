package com.tradingapp.data.repository

import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.mapper.toDomain
import com.tradingapp.data.mapper.toEntity
import com.tradingapp.database.TradingDatabase
import com.tradingapp.database.dao.OrderDao
import com.tradingapp.database.dao.PositionDao
import com.tradingapp.database.dao.WalletDao
import com.tradingapp.database.entity.OrderEntity
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.database.entity.WalletEntity
import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.repository.TradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Open to allow test subclasses to override runInTransaction, bypassing Room's Android-only withTransaction.
@Singleton
open class TradeRepositoryImpl @Inject constructor(
    private val db: TradingDatabase,
    private val orderDao: OrderDao,
    private val positionDao: PositionDao,
    private val walletDao: WalletDao,
    private val dispatchers: DispatcherProvider,
) : TradeRepository {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal open suspend fun <R> runInTransaction(block: suspend () -> R): R = db.withTransaction(block)

    override suspend fun placeOrder(order: Order, takeProfit: Double?, stopLoss: Double?): Result<Order> = runCatching {
        withContext(dispatchers.io) {
            runInTransaction {
                updateWalletForOrder(order)
                updatePositionForOrder(order, takeProfit, stopLoss)
                orderDao.insert(order.toEntity())
                order
            }
        }
    }

    private suspend fun updateWalletForOrder(order: Order) {
        val wallet = walletDao.get() ?: WalletEntity(cashBalance = WalletEntity.INITIAL_BALANCE)
        val newBalance = when (order.side) {
            OrderSide.BUY -> wallet.cashBalance - order.totalValue
            OrderSide.SELL -> wallet.cashBalance + order.totalValue
        }
        walletDao.upsert(wallet.copy(cashBalance = newBalance))
    }

    private suspend fun updatePositionForOrder(order: Order, takeProfit: Double?, stopLoss: Double?) {
        val existing = positionDao.getBySymbol(order.symbol)
        when (order.side) {
            OrderSide.BUY -> applyBuyOrder(order, existing, takeProfit, stopLoss)
            OrderSide.SELL -> applySellOrder(order, existing, takeProfit, stopLoss)
        }
    }

    private suspend fun applyBuyOrder(
        order: Order,
        existing: PositionEntity?,
        takeProfit: Double?,
        stopLoss: Double?,
    ) {
        val newQty = (existing?.quantity ?: 0.0) + order.quantity
        val newAvgPrice = if (existing != null) {
            (existing.quantity * existing.avgPrice + order.quantity * order.price) /
                (existing.quantity + order.quantity)
        } else {
            order.price
        }
        positionDao.upsert(
            PositionEntity(
                id = existing?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                symbol = order.symbol,
                direction = TradeDirection.LONG.name,
                quantity = newQty,
                avgPrice = newAvgPrice,
                takeProfit = takeProfit ?: existing?.takeProfit,
                stopLoss = stopLoss ?: existing?.stopLoss,
                openedAt = existing?.openedAt ?: order.timestamp,
                updatedAt = order.timestamp,
            ),
        )
    }

    private suspend fun applySellOrder(
        order: Order,
        existing: PositionEntity?,
        takeProfit: Double?,
        stopLoss: Double?,
    ) {
        val newQty = (existing?.quantity ?: 0.0) - order.quantity
        if (newQty <= 0.0) {
            positionDao.deleteBySymbol(order.symbol)
            return
        }
        positionDao.upsert(
            PositionEntity(
                id = existing?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                symbol = order.symbol,
                direction = existing?.direction ?: TradeDirection.SHORT.name,
                quantity = newQty,
                avgPrice = existing?.avgPrice ?: order.price,
                takeProfit = takeProfit ?: existing?.takeProfit,
                stopLoss = stopLoss ?: existing?.stopLoss,
                openedAt = existing?.openedAt ?: order.timestamp,
                updatedAt = order.timestamp,
            ),
        )
    }

    override fun observeOrders(): Flow<List<Order>> =
        orderDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeOrdersForSymbol(symbol: String): Flow<List<Order>> =
        orderDao.observeBySymbol(symbol).map { entities -> entities.map { it.toDomain() } }

    override fun observePositions(): Flow<List<Position>> =
        positionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePosition(symbol: String): Flow<Position?> =
        positionDao.observeBySymbol(symbol).map { it?.toDomain() }

    override fun observePositionById(positionId: String): Flow<Position?> =
        positionDao.observeById(positionId).map { it?.toDomain() }

    override fun observeCashBalance(): Flow<Double> =
        walletDao.observe().map { it?.cashBalance ?: WalletEntity.INITIAL_BALANCE }

    override suspend fun getCashBalance(): Double = withContext(dispatchers.io) {
        walletDao.get()?.cashBalance ?: WalletEntity.INITIAL_BALANCE
    }

    override suspend fun updatePositionRisk(positionId: String, takeProfit: Double?, stopLoss: Double?) {
        withContext(dispatchers.io) {
            positionDao.updateRisk(positionId, takeProfit, stopLoss)
        }
    }

    override suspend fun closePosition(positionId: String, closePrice: Double, reason: CloseReason): Result<Unit> =
        runCatching {
            withContext(dispatchers.io) {
                runInTransaction {
                    val entity = positionDao.getById(positionId) ?: return@runInTransaction
                    val direction = runCatching {
                        TradeDirection.valueOf(entity.direction)
                    }.getOrDefault(TradeDirection.LONG)
                    val realizedPnL = when (direction) {
                        TradeDirection.LONG -> (closePrice - entity.avgPrice) * entity.quantity
                        TradeDirection.SHORT -> (entity.avgPrice - closePrice) * entity.quantity
                    }
                    val proceeds = closePrice * entity.quantity
                    val wallet = walletDao.get() ?: WalletEntity(cashBalance = WalletEntity.INITIAL_BALANCE)
                    walletDao.upsert(wallet.copy(cashBalance = wallet.cashBalance + proceeds))

                    val now = System.currentTimeMillis()
                    val closeOrder = OrderEntity(
                        id = UUID.randomUUID().toString(),
                        symbol = entity.symbol,
                        side = if (direction == TradeDirection.LONG) OrderSide.SELL.name else OrderSide.BUY.name,
                        direction = direction.name,
                        quantity = entity.quantity,
                        price = entity.avgPrice,
                        totalValue = entity.quantity * entity.avgPrice,
                        status = OrderStatus.FILLED.name,
                        timestamp = now,
                        closePrice = closePrice,
                        closedAt = now,
                        closeReason = reason.name,
                        realizedPnL = realizedPnL,
                    )
                    orderDao.insert(closeOrder)
                    positionDao.deleteById(positionId)
                }
            }
        }
}
