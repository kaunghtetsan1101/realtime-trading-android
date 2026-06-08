package com.tradingapp.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.common.result.Result
import com.tradingapp.domain.usecase.GetFavoritesUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Watchlist (favourites-only) tab.
 *
 * Deliberately has no [SyncAssetsUseCase] — network sync is the Market tab's responsibility.
 * This ViewModel is a read-only view over the Room favourites query; it reacts automatically
 * when the Market tab (or this screen) toggles a favourite.
 */
@HiltViewModel
class FavoritesViewModel
@Inject
constructor(
    private val getFavorites: GetFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
) : ViewModel() {
    private val stateMutable = MutableStateFlow(WatchlistState())
    val state: StateFlow<WatchlistState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<WatchlistEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    init {
        observeFavorites()
        observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.Refresh -> Unit // no-op: no network sync on this tab
            is WatchlistEvent.ToggleFavorite -> onToggleFavorite(event.symbol, event.isFav)
            is WatchlistEvent.AssetClicked -> sendEffect(WatchlistEffect.NavigateToDetail(event.symbol))
        }
    }

    private fun observeFavorites() {
        getFavorites()
            .onEach { result ->
                when (result) {
                    is Result.Loading -> stateMutable.update { it.copy(isLoading = true, error = null) }
                    is Result.Success ->
                        stateMutable.update {
                            it.copy(isLoading = false, assets = result.data, error = null)
                        }
                    is Result.Error -> stateMutable.update {
                        it.copy(isLoading = false, error = ErrorMapper.toUserMessage(result.exception))
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun onToggleFavorite(symbol: String, isFav: Boolean) {
        viewModelScope.launch {
            runCatching { toggleFavorite(symbol, isFav) }
                .onFailure { sendEffect(WatchlistEffect.ShowSnackbar("Failed to update favourite")) }
        }
    }

    private fun sendEffect(effect: WatchlistEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
