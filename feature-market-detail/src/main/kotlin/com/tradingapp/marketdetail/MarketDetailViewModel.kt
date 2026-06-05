package com.tradingapp.marketdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.common.result.Result
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
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

/**
 * Navigation 3: the route key carries [symbol] directly via [@Assisted] injection,
 * replacing the Nav2 SavedStateHandle look-up. The Factory is wired in [AppNavGraph]
 * via hiltViewModel(creationCallback = { factory -> factory.create(key.symbol) }).
 */
@HiltViewModel(assistedFactory = MarketDetailViewModel.Factory::class)
class MarketDetailViewModel @AssistedInject constructor(
    @Assisted val symbol: String,
    private val getAssetDetail: GetAssetDetailUseCase,
    private val observePriceTicks: ObservePriceTicksUseCase,
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(symbol: String): MarketDetailViewModel
    }

    private val stateMutable = MutableStateFlow(MarketDetailState())
    val state: StateFlow<MarketDetailState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<MarketDetailEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    init {
        load()
        observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MarketDetailEvent) {
        when (event) {
            MarketDetailEvent.Retry -> load()
            MarketDetailEvent.NavigateBack -> sendEffect(MarketDetailEffect.NavigateBack)
            MarketDetailEvent.Trade -> sendEffect(MarketDetailEffect.NavigateToTrade(symbol))
        }
    }

    // --- Private ---

    private fun load() {
        stateMutable.update { it.copy(isLoading = true, error = null) }

        getAssetDetail(symbol)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> stateMutable.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> stateMutable.update {
                        it.copy(isLoading = false, asset = result.data, error = null)
                    }
                    is Result.Error -> stateMutable.update {
                        it.copy(isLoading = false, error = ErrorMapper.toUserMessage(result.exception))
                    }
                }
            }
            .launchIn(viewModelScope)

        observePriceTicks(symbol)
            .onEach { tick ->
                stateMutable.update { s ->
                    s.copy(
                        asset = s.asset?.copy(currentPrice = tick.price),
                        recentPrices = (s.recentPrices + tick.price).takeLast(MAX_PRICE_HISTORY),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sendEffect(effect: MarketDetailEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }

    companion object {
        private const val MAX_PRICE_HISTORY = 50
    }
}
