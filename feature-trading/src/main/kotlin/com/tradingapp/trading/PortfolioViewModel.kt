package com.tradingapp.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.domain.usecase.GetOrderHistoryUseCase
import com.tradingapp.domain.usecase.GetPortfolioUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
) : ViewModel() {

    private val stateMutable = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<PortfolioEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    private var portfolioJob: Job? = null
    private var ordersJob: Job? = null

    init {
        observePortfolio()
        observeOrders()
        observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.NavigateBack -> sendEffect(PortfolioEffect.NavigateBack)
            is PortfolioEvent.TradeAsset -> sendEffect(PortfolioEffect.NavigateToTrade(event.symbol))
            PortfolioEvent.Retry -> {
                stateMutable.update { it.copy(error = null, isLoading = true) }
                observePortfolio()
                observeOrders()
            }
        }
    }

    // --- Private ---

    private fun observePortfolio() {
        portfolioJob?.cancel()
        portfolioJob = getPortfolio()
            .catch { e ->
                stateMutable.update { it.copy(error = ErrorMapper.toUserMessage(e), isLoading = false) }
            }
            .onEach { portfolio ->
                stateMutable.update {
                    it.copy(portfolio = portfolio, isLoading = false, error = null, lastSyncedAt = System.currentTimeMillis())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeOrders() {
        ordersJob?.cancel()
        ordersJob = getOrderHistory()
            .catch { /* orders are non-critical — silently ignore */ }
            .onEach { orders -> stateMutable.update { it.copy(orders = orders) } }
            .launchIn(viewModelScope)
    }

    private fun sendEffect(effect: PortfolioEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
