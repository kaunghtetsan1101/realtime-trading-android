package com.tradingapp.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.domain.usecase.GetOrderHistoryUseCase
import com.tradingapp.domain.usecase.GetPortfolioUseCase
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
class PortfolioViewModel @Inject constructor(
    private val getPortfolio: GetPortfolioUseCase,
    private val getOrderHistory: GetOrderHistoryUseCase,
) : ViewModel() {

    private val stateMutable = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<PortfolioEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    init {
        observePortfolio()
        observeOrders()
    }

    fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.NavigateBack -> sendEffect(PortfolioEffect.NavigateBack)
            is PortfolioEvent.TradeAsset -> sendEffect(PortfolioEffect.NavigateToTrade(event.symbol))
        }
    }

    // --- Private ---

    private fun observePortfolio() {
        getPortfolio()
            .onEach { portfolio ->
                stateMutable.update { it.copy(portfolio = portfolio, isLoading = false, error = null) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeOrders() {
        getOrderHistory()
            .onEach { orders -> stateMutable.update { it.copy(orders = orders) } }
            .launchIn(viewModelScope)
    }

    private fun sendEffect(effect: PortfolioEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
