package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.Portfolio
import com.tradingapp.domain.repository.AssetRepository
import com.tradingapp.domain.repository.TradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetPortfolioUseCase @Inject constructor(
    private val tradeRepository: TradeRepository,
    private val assetRepository: AssetRepository,
) {
    operator fun invoke(): Flow<Portfolio> = combine(
        tradeRepository.observePositions(),
        assetRepository.observeAssets(),
        tradeRepository.observeCashBalance(),
    ) { positions, assets, cashBalance ->
        val priceMap = assets.associateBy({ it.symbol }, { it.currentPrice })
        val enrichedPositions = positions.map { position ->
            val livePrice = priceMap[position.symbol] ?: position.averagePrice
            val totalValue = position.quantity * livePrice
            val unrealizedPnL = when (position.direction) {
                com.tradingapp.domain.model.TradeDirection.LONG -> (livePrice - position.averagePrice) * position.quantity
                com.tradingapp.domain.model.TradeDirection.SHORT -> (position.averagePrice - livePrice) * position.quantity
            }
            val unrealizedPnLPct = if (position.averagePrice > 0.0) {
                unrealizedPnL / (position.averagePrice * position.quantity) * 100.0
            } else {
                0.0
            }
            position.copy(
                currentPrice = livePrice,
                totalValue = totalValue,
                unrealizedPnL = unrealizedPnL,
                unrealizedPnLPct = unrealizedPnLPct,
            )
        }
        val positionsTotal = enrichedPositions.sumOf { it.totalValue }
        val totalValue = cashBalance + positionsTotal
        val totalUnrealizedPnL = enrichedPositions.sumOf { it.unrealizedPnL }
        val costBasis = enrichedPositions.sumOf { it.averagePrice * it.quantity }
        val totalUnrealizedPnLPct = if (costBasis > 0.0) totalUnrealizedPnL / costBasis * 100.0 else 0.0
        Portfolio(
            cashBalance = cashBalance,
            positions = enrichedPositions,
            totalValue = totalValue,
            totalUnrealizedPnL = totalUnrealizedPnL,
            totalUnrealizedPnLPct = totalUnrealizedPnLPct,
        )
    }
}
