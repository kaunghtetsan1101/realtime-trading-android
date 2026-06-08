package com.tradingapp.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.domain.model.CloseReason
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.usecase.ClosePositionUseCase
import com.tradingapp.domain.usecase.EditPositionRiskUseCase
import com.tradingapp.domain.usecase.GetOrderHistoryUseCase
import com.tradingapp.domain.usecase.GetPortfolioUseCase
import com.tradingapp.domain.usecase.MonitorPositionExitUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import com.tradingapp.domain.usecase.ValidateTakeProfitStopLossUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getPortfolio: GetPortfolioUseCase,
    private val getOrderHistory: GetOrderHistoryUseCase,
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
    private val observePriceTicks: ObservePriceTicksUseCase,
    private val monitorPositionExit: MonitorPositionExitUseCase,
    private val closePosition: ClosePositionUseCase,
    private val editPositionRisk: EditPositionRiskUseCase,
    private val validateTpSl: ValidateTakeProfitStopLossUseCase,
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
        monitorExits()
        observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PortfolioEvent) {
        when (event) {
            is PortfolioEvent.TradeAsset -> sendEffect(PortfolioEffect.NavigateToTrade(event.symbol))
            is PortfolioEvent.EditPosition -> stateMutable.update { it.copy(editingPosition = event.position) }
            PortfolioEvent.DismissEditPosition -> stateMutable.update { it.copy(editingPosition = null) }
            is PortfolioEvent.SavePositionRisk -> onSavePositionRisk(event.positionId, event.takeProfitStr, event.stopLossStr)
            is PortfolioEvent.ClosePosition -> onClosePosition(event.positionId)
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

    // Monitors all open positions concurrently; auto-closes when TP or SL is triggered.
    private fun monitorExits() {
        getPortfolio()
            .map { it.positions }
            .flatMapLatest { positions ->
                if (positions.isEmpty()) return@flatMapLatest emptyFlow()
                merge(*positions.map { position ->
                    val priceFlow = observePriceTicks(position.symbol).map { it.price }
                    monitorPositionExit(position, priceFlow)
                        .take(1)
                        .map { reason -> Triple(position.id, position.symbol, reason) }
                }.toTypedArray())
            }
            .onEach { (positionId, symbol, reason) ->
                val currentPrice = stateMutable.value.portfolio?.positions
                    ?.firstOrNull { it.id == positionId }?.currentPrice ?: 0.0
                closePosition(positionId, currentPrice, reason)
                val reasonLabel = when (reason) {
                    CloseReason.TAKE_PROFIT_TRIGGERED -> "Take Profit"
                    CloseReason.STOP_LOSS_TRIGGERED -> "Stop Loss"
                    CloseReason.MANUAL_CLOSE -> "Manual Close"
                }
                val pnl = stateMutable.value.portfolio?.positions
                    ?.firstOrNull { it.id == positionId }?.unrealizedPnL
                val pnlText = pnl?.let { v ->
                    val prefix = if (v >= 0) "+" else ""
                    " PnL: $prefix$${"%.2f".format(v)}"
                } ?: ""
                sendEffect(PortfolioEffect.ShowSnackbar("$symbol position closed — $reasonLabel hit.$pnlText"))
            }
            .catch { /* monitoring errors are non-critical */ }
            .launchIn(viewModelScope)
    }

    private fun onSavePositionRisk(positionId: String, takeProfitStr: String, stopLossStr: String) {
        val position = stateMutable.value.editingPosition ?: return
        viewModelScope.launch {
            val result = editPositionRisk(positionId, position.direction, position.averagePrice, takeProfitStr, stopLossStr)
            result.fold(
                onSuccess = {
                    stateMutable.update { it.copy(editingPosition = null) }
                    sendEffect(PortfolioEffect.ShowSnackbar("Risk settings updated"))
                },
                onFailure = { e ->
                    sendEffect(PortfolioEffect.ShowSnackbar("Invalid values: ${e.message}"))
                },
            )
        }
    }

    private fun onClosePosition(positionId: String) {
        val position = stateMutable.value.portfolio?.positions?.firstOrNull { it.id == positionId } ?: return
        viewModelScope.launch {
            closePosition(positionId, position.currentPrice, CloseReason.MANUAL_CLOSE).fold(
                onSuccess = {
                    val pnlText = "+$${"%.2f".format(position.unrealizedPnL)}".let { v ->
                        if (position.unrealizedPnL < 0) "-$${"%.2f".format(-position.unrealizedPnL)}" else v
                    }
                    sendEffect(PortfolioEffect.ShowSnackbar("${position.symbol} position closed. PnL: $pnlText"))
                },
                onFailure = { sendEffect(PortfolioEffect.ShowSnackbar("Failed to close position")) },
            )
        }
    }

    private fun sendEffect(effect: PortfolioEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
