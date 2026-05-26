package com.tradingapp.marketdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.result.Result
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
 * Navigation 3: the route key carries [symbol] directly via [@Assisted] injection,
 * replacing the Nav2 SavedStateHandle look-up. The Factory is wired in [AppNavGraph]
 * via hiltViewModel(creationCallback = { factory -> factory.create(key.symbol) }).
 */
@HiltViewModel(assistedFactory = MarketDetailViewModel.Factory::class)
class MarketDetailViewModel @AssistedInject constructor(
    @Assisted val symbol: String,
    private val getAssetDetail:    GetAssetDetailUseCase,
    private val observePriceTicks: ObservePriceTicksUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(symbol: String): MarketDetailViewModel
    }

    private val _state   = MutableStateFlow(MarketDetailState())
    val state: StateFlow<MarketDetailState> = _state.asStateFlow()

    private val _effects = Channel<MarketDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { load() }

    fun onEvent(event: MarketDetailEvent) {
        when (event) {
            MarketDetailEvent.Retry        -> load()
            MarketDetailEvent.NavigateBack -> sendEffect(MarketDetailEffect.NavigateBack)
        }
    }

    // --- Private ---

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }

        getAssetDetail(symbol)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> _state.update { it.copy(isLoading = false, asset = result.data, error = null) }
                    is Result.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)

        observePriceTicks(symbol)
            .onEach { tick ->
                _state.update { s ->
                    s.copy(
                        asset        = s.asset?.copy(currentPrice = tick.price),
                        recentPrices = (s.recentPrices + tick.price).takeLast(MAX_PRICE_HISTORY),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sendEffect(effect: MarketDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        private const val MAX_PRICE_HISTORY = 50
    }
}
