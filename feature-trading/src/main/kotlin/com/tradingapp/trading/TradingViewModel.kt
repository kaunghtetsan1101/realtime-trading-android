package com.tradingapp.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.error.ErrorMapper
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.domain.model.ValidationResult
import com.tradingapp.domain.repository.TradeRepository
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import com.tradingapp.domain.usecase.PlaceOrderUseCase
import com.tradingapp.domain.usecase.ValidateOrderUseCase
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
    private val getAssetDetail: GetAssetDetailUseCase,
    private val observePriceTicks: ObservePriceTicksUseCase,
    private val validateOrder: ValidateOrderUseCase,
    private val placeOrder: PlaceOrderUseCase,
    private val tradeRepository: TradeRepository,
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
    }

    fun onEvent(event: TradingEvent) {
        when (event) {
            is TradingEvent.SideSelected -> onSideSelected(event.side)
            is TradingEvent.QuantityChanged -> onQuantityChanged(event.quantity)
            is TradingEvent.QuickFillSelected -> onQuickFill(event.fraction)
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
        loadJob = getAssetDetail(symbol)
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
        observePriceTicks(symbol)
            .onEach { tick -> updateAndValidate { it.copy(currentPrice = tick.price) } }
            .launchIn(viewModelScope)
    }

    private fun observeCashBalance() {
        tradeRepository.observeCashBalance()
            .onEach { balance -> updateAndValidate { it.copy(cashBalance = balance) } }
            .launchIn(viewModelScope)
    }

    private fun observePosition() {
        tradeRepository.observePosition(symbol)
            .onEach { position -> updateAndValidate { it.copy(existingPosition = position) } }
            .launchIn(viewModelScope)
    }

    private fun onSideSelected(side: OrderSide) {
        updateAndValidate { it.copy(selectedSide = side, isReviewVisible = false) }
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
            placeOrder(
                side = s.selectedSide,
                symbol = symbol,
                quantity = qty,
                executionPrice = s.currentPrice,
            ).fold(
                onSuccess = {
                    stateMutable.update { it.copy(isPlacingOrder = false, isReviewVisible = false) }
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
            new.copy(validationError = computeValidationError(new))
        }
    }

    private fun computeValidationError(s: TradingState): ValidationError? {
        val qty = s.quantityInput
        if (qty.isBlank()) return null
        return when (
            val result = validateOrder(
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

    private fun sendEffect(effect: TradingEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }

    companion object {
        fun formatQuantity(qty: Double): String = if (qty <= 0.0) "0" else "%.8f".format(qty).trimEnd('0').trimEnd('.')
    }
}
