package com.tradingapp.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.usecase.GetWatchlistUseCase
import com.tradingapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getWatchlist: GetWatchlistUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val stateMutable = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = stateMutable.asStateFlow()

    // Channel-based effects: consumed once, not replayed on recomposition.
    private val effectsMutable = Channel<SearchEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    // Three separate input flows prevent state feedback loops when reading/writing
    // the same MutableStateFlow inside a combine transform.
    private val queryMutable = MutableStateFlow("")
    private val filterInputMutable = MutableStateFlow(AssetFilter.ALL)
    private val sortInputMutable = MutableStateFlow(SortOrder.NONE)

    init {
        // Immediate query display: TextField stays responsive while the result list catches up.
        queryMutable
            .onEach { q -> stateMutable.update { it.copy(query = q) } }
            .launchIn(viewModelScope)

        // Debounced combine drives filtered results. Query is debounced; filter/sort are not
        // (they are discrete tap actions — debounce adds latency with no benefit).
        combine(
            getWatchlist(),
            queryMutable.debounce(300).distinctUntilChanged(),
            filterInputMutable,
            sortInputMutable,
        ) { result, query, filter, sort ->
            SearchCombined(result, query, filter, sort)
        }
            .onEach { (result, query, filter, sort) ->
                stateMutable.update { s ->
                    when (result) {
                        is Result.Loading -> s.copy(isLoading = true, error = null)
                        is Result.Error -> s.copy(isLoading = false, error = result.message)
                        is Result.Success -> s.copy(
                            isLoading = false,
                            error = null,
                            results = result.data.applySearchFilters(query, filter, sort),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> queryMutable.value = event.query
            is SearchEvent.ClearQuery -> queryMutable.value = ""
            is SearchEvent.FilterSelected -> {
                // Immediate state update for chip highlight; input flow drives the combine.
                stateMutable.update { it.copy(activeFilter = event.filter) }
                filterInputMutable.value = event.filter
            }
            is SearchEvent.SortSelected -> {
                stateMutable.update { it.copy(sortOrder = event.sort) }
                sortInputMutable.value = event.sort
            }
            is SearchEvent.AssetClicked -> sendEffect(SearchEffect.NavigateToDetail(event.symbol))
            is SearchEvent.ToggleFavorite -> onToggleFavorite(event.symbol, event.isFav)
        }
    }

    // --- Private ---

    private fun onToggleFavorite(symbol: String, isFav: Boolean) {
        viewModelScope.launch {
            runCatching { toggleFavorite(symbol, isFav) }
                .onFailure { sendEffect(SearchEffect.ShowSnackbar("Failed to update favorite")) }
        }
    }

    private fun sendEffect(effect: SearchEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}

// Carries the four combined flow values — keeps the combine transform readable.
private data class SearchCombined(
    val result: Result<List<Asset>>,
    val query: String,
    val filter: AssetFilter,
    val sort: SortOrder,
)

// Filter logic lives in the ViewModel (not a use case) because AssetFilter/SortOrder are
// feature-specific UI concerns that would pollute the domain layer.
private fun List<Asset>.applySearchFilters(query: String, filter: AssetFilter, sort: SortOrder): List<Asset> {
    val q = query.trim()
    return this
        .filter { asset ->
            val textMatch = q.isBlank() ||
                asset.symbol.contains(q, ignoreCase = true) ||
                asset.name.contains(q, ignoreCase = true)
            val categoryMatch = when (filter) {
                AssetFilter.ALL -> true
                AssetFilter.FAVORITES -> asset.isFavorite
                AssetFilter.GAINERS -> asset.priceChangePct24h > 0
                AssetFilter.LOSERS -> asset.priceChangePct24h < 0
            }
            textMatch && categoryMatch
        }
        .let { filtered ->
            when (sort) {
                SortOrder.NONE -> filtered
                SortOrder.PRICE_CHANGE_DESC -> filtered.sortedByDescending { it.priceChangePct24h }
                SortOrder.PRICE_CHANGE_ASC -> filtered.sortedBy { it.priceChangePct24h }
                SortOrder.VOLUME_DESC -> filtered.sortedByDescending { it.volume24h }
                SortOrder.VOLUME_ASC -> filtered.sortedBy { it.volume24h }
            }
        }
}
