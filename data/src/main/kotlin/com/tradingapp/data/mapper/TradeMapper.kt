package com.tradingapp.data.mapper

import com.tradingapp.database.entity.OrderEntity
import com.tradingapp.database.entity.PositionEntity
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Position

fun OrderEntity.toDomain(): Order = Order(
    id = id,
    symbol = symbol,
    side = OrderSide.valueOf(side),
    quantity = quantity,
    price = price,
    totalValue = totalValue,
    status = OrderStatus.valueOf(status),
    timestamp = timestamp,
)

fun Order.toEntity(): OrderEntity = OrderEntity(
    id = id,
    symbol = symbol,
    side = side.name,
    quantity = quantity,
    price = price,
    totalValue = totalValue,
    status = status.name,
    timestamp = timestamp,
)

fun PositionEntity.toDomain(): Position = Position(
    symbol = symbol,
    quantity = quantity,
    averagePrice = avgPrice,
)
