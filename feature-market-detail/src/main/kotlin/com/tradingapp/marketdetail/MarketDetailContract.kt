package com.tradingapp.marketdetail

import com.tradingapp.domain.model.Asset

/**
 * MVI contract for the Market Detail feature.
 *
 * [MarketDetailState]  — single source of truth for the detail UI.
 * [MarketDetailEvent]  — user intents (retry, back navigation).
 * [MarketDetailEffect] — one-shot side effects consumed by the UI.
 */

data class MarketDetailState(
    val asset: Asset? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Kept in memory for the live price ticker; chart data is served by TradingView WebView.
    val recentPrices: List<Double> = emptyList(),
    val isOffline: Boolean = false,
)

sealed interface MarketDetailEvent {
    data object Retry : MarketDetailEvent

    data object NavigateBack : MarketDetailEvent
}

sealed interface MarketDetailEffect {
    data object NavigateBack : MarketDetailEffect
}
