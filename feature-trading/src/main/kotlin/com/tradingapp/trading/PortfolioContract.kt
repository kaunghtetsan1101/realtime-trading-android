package com.tradingapp.trading

import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.Portfolio

data class PortfolioState(
    val portfolio: Portfolio? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastSyncedAt: Long? = null,
)

sealed interface PortfolioEvent {
    data object NavigateBack : PortfolioEvent
    data class TradeAsset(val symbol: String) : PortfolioEvent
    data object Retry : PortfolioEvent
}

sealed interface PortfolioEffect {
    data object NavigateBack : PortfolioEffect
    data class NavigateToTrade(val symbol: String) : PortfolioEffect
}
