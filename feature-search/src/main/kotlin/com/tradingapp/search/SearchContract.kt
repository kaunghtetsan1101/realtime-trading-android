package com.tradingapp.search

import com.tradingapp.domain.model.Asset

/**
 * MVI contract for the Search feature.
 *
 * [SearchState]  — single source of truth for the UI.
 * [SearchEvent]  — user-driven intents.
 * [SearchEffect] — one-shot side effects (navigation, snackbar).
 */

data class SearchState(
    val query: String = "",
    val activeFilter: AssetFilter = AssetFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NONE,
    val results: List<Asset> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

enum class AssetFilter { ALL, FAVORITES, GAINERS, LOSERS }

enum class SortOrder { NONE, PRICE_CHANGE_DESC, PRICE_CHANGE_ASC, VOLUME_DESC, VOLUME_ASC }

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent

    data class FilterSelected(val filter: AssetFilter) : SearchEvent

    data class SortSelected(val sort: SortOrder) : SearchEvent

    data class AssetClicked(val symbol: String) : SearchEvent

    data class ToggleFavorite(val symbol: String, val isFav: Boolean) : SearchEvent

    data object ClearQuery : SearchEvent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val symbol: String) : SearchEffect

    data object NavigateBack : SearchEffect

    data class ShowSnackbar(val message: String) : SearchEffect
}
