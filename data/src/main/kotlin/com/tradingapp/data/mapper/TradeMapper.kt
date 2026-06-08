package com.tradingapp.data.mapper

import com.tradingapp.database.entity.OrderEntity
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.TradeDirection

fun OrderEntity.toDomain(): Order = Order(
    id = id,
    symbol = symbol,
    side = OrderSide.valueOf(side),
    direction = runCatching { TradeDirection.valueOf(direction) }.getOrDefault(TradeDirection.LONG),
    quantity = quantity,
    price = price,
    totalValue = totalValue,
    status = OrderStatus.valueOf(status),
    timestamp = timestamp,
    closePrice = closePrice,
    closedAt = closedAt,
    closeReason = closeReason?.let { runCatching { CloseReason.valueOf(it) }.getOrNull() },
    realizedPnL = realizedPnL,
)

fun Order.toEntity(): OrderEntity = OrderEntity(
    id = id,
    symbol = symbol,
    side = side.name,
    direction = direction.name,
    quantity = quantity,
    price = price,
    totalValue = totalValue,
    status = status.name,
    timestamp = timestamp,
    closePrice = closePrice,
    closedAt = closedAt,
    closeReason = closeReason?.name,
    realizedPnL = realizedPnL,
)

fun PositionEntity.toDomain(): Position = Position(
    id = id,
    symbol = symbol,
    direction = runCatching { TradeDirection.valueOf(direction) }.getOrDefault(TradeDirection.LONG),
    quantity = quantity,
    averagePrice = avgPrice,
    takeProfit = takeProfit,
    stopLoss = stopLoss,
    openedAt = openedAt,
)
