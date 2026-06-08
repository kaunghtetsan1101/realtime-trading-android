package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.repository.TradeRepository
import javax.inject.Inject

class ClosePositionUseCase @Inject constructor(
    private val tradeRepository: TradeRepository,
) {
    suspend operator fun invoke(
        positionId: String,
        closePrice: Double,
        reason: CloseReason,
    ): Result<Unit> = tradeRepository.closePosition(positionId, closePrice, reason)
}
