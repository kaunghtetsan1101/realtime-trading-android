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
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.database.entity.WalletEntity
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.repository.TradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    override suspend fun placeOrder(order: Order): Result<Order> = runCatching {
        withContext(dispatchers.io) {
            runInTransaction {
                val wallet = walletDao.get() ?: WalletEntity(cashBalance = WalletEntity.INITIAL_BALANCE)

                val newBalance = when (order.side) {
                    OrderSide.BUY -> wallet.cashBalance - order.totalValue
                    OrderSide.SELL -> wallet.cashBalance + order.totalValue
                }
                walletDao.upsert(wallet.copy(cashBalance = newBalance))

                val existing = positionDao.getBySymbol(order.symbol)
                when (order.side) {
                    OrderSide.BUY -> {
                        val newQty = (existing?.quantity ?: 0.0) + order.quantity
                        val newAvgPrice = if (existing != null) {
                            (existing.quantity * existing.avgPrice + order.quantity * order.price) /
                                (existing.quantity + order.quantity)
                        } else {
                            order.price
                        }
                        positionDao.upsert(
                            PositionEntity(
                                symbol = order.symbol,
                                quantity = newQty,
                                avgPrice = newAvgPrice,
                                updatedAt = order.timestamp,
                            ),
                        )
                    }
                    OrderSide.SELL -> {
                        val newQty = (existing?.quantity ?: 0.0) - order.quantity
                        if (newQty <= 0.0) {
                            positionDao.deleteBySymbol(order.symbol)
                        } else {
                            positionDao.upsert(
                                PositionEntity(
                                    symbol = order.symbol,
                                    quantity = newQty,
                                    avgPrice = existing?.avgPrice ?: order.price,
                                    updatedAt = order.timestamp,
                                ),
                            )
                        }
                    }
                }

                orderDao.insert(order.toEntity())
                order
            }
        }
    }

    override fun observeOrders(): Flow<List<Order>> =
        orderDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeOrdersForSymbol(symbol: String): Flow<List<Order>> =
        orderDao.observeBySymbol(symbol).map { entities -> entities.map { it.toDomain() } }

    override fun observePositions(): Flow<List<Position>> =
        positionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePosition(symbol: String): Flow<Position?> =
        positionDao.observeBySymbol(symbol).map { it?.toDomain() }

    override fun observeCashBalance(): Flow<Double> =
        walletDao.observe().map { it?.cashBalance ?: WalletEntity.INITIAL_BALANCE }

    override suspend fun getCashBalance(): Double = withContext(dispatchers.io) {
        walletDao.get()?.cashBalance ?: WalletEntity.INITIAL_BALANCE
    }
}
