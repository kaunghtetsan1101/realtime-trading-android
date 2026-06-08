package com.tradingapp.trading

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.Portfolio
import com.tradingapp.domain.model.Position

data class PortfolioState(
    val portfolio: Portfolio? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastSyncedAt: Long? = null,
    val editingPosition: Position? = null,
)

sealed interface PortfolioEvent {
    data class TradeAsset(val symbol: String) : PortfolioEvent
    data class EditPosition(val position: Position) : PortfolioEvent
    data class ClosePosition(val positionId: String) : PortfolioEvent
    data class SavePositionRisk(val positionId: String, val takeProfitStr: String, val stopLossStr: String) : PortfolioEvent
    data object DismissEditPosition : PortfolioEvent
    data object Retry : PortfolioEvent
}

sealed interface PortfolioEffect {
    data class NavigateToTrade(val symbol: String) : PortfolioEffect
    data class ShowSnackbar(val message: String) : PortfolioEffect
}
