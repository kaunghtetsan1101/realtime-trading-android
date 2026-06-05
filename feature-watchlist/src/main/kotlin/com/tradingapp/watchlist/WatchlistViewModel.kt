package com.tradingapp.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.common.result.Result
import com.tradingapp.domain.usecase.GetWatchlistUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.SyncAssetsUseCase
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

@HiltViewModel
class WatchlistViewModel
@Inject
constructor(
    private val getWatchlist: GetWatchlistUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val syncAssets: SyncAssetsUseCase,
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
) : ViewModel() {
    private val stateMutable = MutableStateFlow(WatchlistState())
    val state: StateFlow<WatchlistState> = stateMutable.asStateFlow()

    // Channel-based effects: consumed once, not replayed on recomposition.
    private val effectsMutable = Channel<WatchlistEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    init {
        observeWatchlist()
        observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
        syncRemote()
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.Refresh -> syncRemote()
            is WatchlistEvent.ToggleFavorite -> onToggleFavorite(event.symbol, event.isFav)
            is WatchlistEvent.AssetClicked -> sendEffect(WatchlistEffect.NavigateToDetail(event.symbol))
        }
    }

    // --- Private ---

    private fun observeWatchlist() {
        getWatchlist()
            .onEach { result ->
                when (result) {
                    is Result.Loading -> stateMutable.update { it.copy(isLoading = true, error = null) }
                    is Result.Success ->
                        stateMutable.update {
                            it.copy(
                                isLoading = false,
                                assets = result.data,
                                error = null,
                            )
                        }
                    is Result.Error -> stateMutable.update {
                        it.copy(isLoading = false, error = ErrorMapper.toUserMessage(result.exception))
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun syncRemote() {
        viewModelScope.launch {
            syncAssets().onFailure { error ->
                // Non-fatal: show snackbar but keep cached data visible
                sendEffect(WatchlistEffect.ShowSnackbar(ErrorMapper.toUserMessage(error)))
            }
        }
    }

    private fun onToggleFavorite(symbol: String, isFav: Boolean) {
        viewModelScope.launch {
            runCatching { toggleFavorite(symbol, isFav) }
                .onFailure { sendEffect(WatchlistEffect.ShowSnackbar("Failed to update favorite")) }
        }
    }

    private fun sendEffect(effect: WatchlistEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
