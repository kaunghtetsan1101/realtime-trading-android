package com.tradingapp.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.TradeDirection
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TradingViewModel.Factory::class)
class TradingViewModel @AssistedInject constructor(
    @Assisted val symbol: String,
    private val deps: TradingViewModelDependencies,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(symbol: String): TradingViewModel
    }

    private val stateMutable = MutableStateFlow(TradingState(symbol = symbol))
    val state: StateFlow<TradingState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<TradingEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadAsset()
        observeLivePrice()
        observeCashBalance()
        observePosition()
        deps.observeNetworkStatus()
            .onEach { isOnline -> stateMutable.update { it.copy(isOffline = !isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: TradingEvent) {
        when (event) {
            is TradingEvent.SideSelected -> onSideSelected(event.side)
            is TradingEvent.QuantityChanged -> onQuantityChanged(event.quantity)
            is TradingEvent.QuickFillSelected -> onQuickFill(event.fraction)
            is TradingEvent.TakeProfitChanged -> onTakeProfitChanged(event.value)
            is TradingEvent.StopLossChanged -> onStopLossChanged(event.value)
            TradingEvent.ReviewOrder -> stateMutable.update { it.copy(isReviewVisible = true) }
            TradingEvent.DismissReview -> stateMutable.update { it.copy(isReviewVisible = false) }
            TradingEvent.ConfirmOrder -> onConfirmOrder()
            TradingEvent.Retry -> loadAsset()
            TradingEvent.NavigateBack -> sendEffect(TradingEffect.NavigateBack)
        }
    }

    // --- Private ---

    private fun loadAsset() {
        loadJob?.cancel()
        stateMutable.update { it.copy(isLoading = true, error = null) }
        loadJob = deps.getAssetDetail(symbol)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> stateMutable.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val asset = result.data
                        updateAndValidate { s ->
                            s.copy(
                                assetName = asset?.name ?: symbol,
                                currentPrice = asset?.currentPrice ?: 0.0,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
                    is Result.Error -> stateMutable.update {
                        it.copy(isLoading = false, error = ErrorMapper.toUserMessage(result.exception))
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLivePrice() {
        deps.observePriceTicks(symbol)
            .onEach { tick ->
                updateAndValidate { it.copy(currentPrice = tick.price, lastSyncedAt = System.currentTimeMillis()) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeCashBalance() {
        deps.tradeRepository.observeCashBalance()
            .onEach { balance -> updateAndValidate { it.copy(cashBalance = balance) } }
            .launchIn(viewModelScope)
    }

    private fun observePosition() {
        deps.tradeRepository.observePosition(symbol)
            .onEach { position -> updateAndValidate { it.copy(existingPosition = position) } }
            .launchIn(viewModelScope)
    }

    private fun onSideSelected(side: OrderSide) {
        updateAndValidate {
            it.copy(
                selectedSide = side,
                isReviewVisible = false,
                takeProfitInput = "",
                stopLossInput = "",
                takeProfitError = null,
                stopLossError = null,
            )
        }
    }

    private fun onTakeProfitChanged(input: String) {
        val sanitized = sanitizeDecimalInput(input)
        updateAndValidate { it.copy(takeProfitInput = sanitized) }
    }

    private fun onStopLossChanged(input: String) {
        val sanitized = sanitizeDecimalInput(input)
        updateAndValidate { it.copy(stopLossInput = sanitized) }
    }

    private fun sanitizeDecimalInput(input: String): String {
        val s = input.filter { it.isDigit() || it == '.' }
        val dotIndex = s.indexOf('.')
        return if (dotIndex ==
            -1
        ) {
            s
        } else {
            s.substring(0, dotIndex + 1) + s.substring(dotIndex + 1).filter { it.isDigit() }
        }
    }

    private fun onQuantityChanged(input: String) {
        val sanitized = input.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIndex = s.indexOf('.')
                if (dotIndex == -1) {
                    s
                } else {
                    s.substring(0, dotIndex + 1) + s.substring(dotIndex + 1).filter { it.isDigit() }
                }
            }
        updateAndValidate { it.copy(quantityInput = sanitized) }
    }

    private fun onQuickFill(fraction: Double) {
        val s = stateMutable.value
        val qty = when (s.selectedSide) {
            OrderSide.BUY -> if (s.currentPrice > 0.0) (s.cashBalance * fraction) / s.currentPrice else 0.0
            OrderSide.SELL -> (s.existingPosition?.quantity ?: 0.0) * fraction
        }
        val formatted = if (qty > 0.0) formatQuantity(qty) else ""
        updateAndValidate { it.copy(quantityInput = formatted) }
    }

    private fun onConfirmOrder() {
        val s = stateMutable.value
        val qty = s.quantityInput.toDoubleOrNull() ?: return
        stateMutable.update { it.copy(isPlacingOrder = true) }
        viewModelScope.launch {
            deps.placeOrder(
                side = s.selectedSide,
                symbol = symbol,
                quantity = qty,
                executionPrice = s.currentPrice,
                takeProfit = s.takeProfitInput.toDoubleOrNull(),
                stopLoss = s.stopLossInput.toDoubleOrNull(),
            ).fold(
                onSuccess = {
                    stateMutable.update {
                        it.copy(
                            isPlacingOrder = false,
                            isReviewVisible = false,
                            takeProfitInput = "",
                            stopLossInput = "",
                        )
                    }
                    sendEffect(TradingEffect.ShowSnackbar("Order placed — check your portfolio"))
                },
                onFailure = { error ->
                    stateMutable.update { it.copy(isPlacingOrder = false, isReviewVisible = false) }
                    sendEffect(TradingEffect.ShowSnackbar("Order failed: ${ErrorMapper.toUserMessage(error)}"))
                },
            )
        }
    }

    private fun updateAndValidate(transform: (TradingState) -> TradingState) {
        stateMutable.update { old ->
            val new = transform(old)
            val (tpError, slError) = computeTpSlErrors(new)
            new.copy(
                validationError = computeValidationError(new),
                takeProfitError = tpError,
                stopLossError = slError,
            )
        }
    }

    private fun computeValidationError(s: TradingState): ValidationError? {
        val qty = s.quantityInput
        if (qty.isBlank()) return null
        return when (
            val result = deps.validateOrder(
                side = s.selectedSide,
                quantityStr = qty,
                currentPrice = s.currentPrice,
                cashBalance = s.cashBalance,
                existingPosition = s.existingPosition,
            )
        ) {
            ValidationResult.Valid -> null
            is ValidationResult.Invalid -> result.error
        }
    }

    private fun computeTpSlErrors(s: TradingState): Pair<ValidationError?, ValidationError?> {
        val entryPrice = s.currentPrice
        if (entryPrice <= 0.0) return null to null
        val direction = if (s.selectedSide == OrderSide.BUY) TradeDirection.LONG else TradeDirection.SHORT
        val result = deps.validateTpSl(direction, entryPrice, s.takeProfitInput, s.stopLossInput)
        if (result == ValidationResult.Valid) return null to null
        return when (val error = (result as ValidationResult.Invalid).error) {
            ValidationError.TAKE_PROFIT_MUST_BE_ABOVE_ENTRY,
            ValidationError.TAKE_PROFIT_MUST_BE_BELOW_ENTRY,
            ValidationError.INVALID_TAKE_PROFIT,
            ValidationError.TAKE_PROFIT_EQUALS_STOP_LOSS,
            -> error to null
            ValidationError.STOP_LOSS_MUST_BE_BELOW_ENTRY,
            ValidationError.STOP_LOSS_MUST_BE_ABOVE_ENTRY,
            ValidationError.INVALID_STOP_LOSS,
            -> null to error
            else -> null to null
        }
    }

    private fun sendEffect(effect: TradingEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }

    companion object {
        fun formatQuantity(qty: Double): String = if (qty <= 0.0) "0" else "%.8f".format(qty).trimEnd('0').trimEnd('.')
    }
}
