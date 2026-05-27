package com.tradingapp.watchlist

import com.tradingapp.domain.model.Asset

/**
 * MVI contract for the Watchlist feature.
 *
 * [WatchlistState]  — the single source of truth for the UI.
 * [WatchlistEvent]  — user-driven intents (one-shot inputs).
 * [WatchlistEffect] — one-time side effects (navigation, snackbar).
 */

data class WatchlistState(
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
)

sealed interface WatchlistEvent {
    data object Refresh : WatchlistEvent

    data class ToggleFavorite(val symbol: String, val isFav: Boolean) : WatchlistEvent

    data class AssetClicked(val symbol: String) : WatchlistEvent
}

sealed interface WatchlistEffect {
    data class NavigateToDetail(val symbol: String) : WatchlistEffect

    data class ShowSnackbar(val message: String) : WatchlistEffect
}
