package com.tradingapp.trading

import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.ValidationError

data class TradingState(
    val symbol: String = "",
    val assetName: String = "",
    val currentPrice: Double = 0.0,
    val cashBalance: Double = 0.0,
    val existingPosition: Position? = null,
    val selectedSide: OrderSide = OrderSide.BUY,
    val quantityInput: String = "",
    val takeProfitInput: String = "",
    val stopLossInput: String = "",
    val validationError: ValidationError? = null,
    val takeProfitError: ValidationError? = null,
    val stopLossError: ValidationError? = null,
    val isReviewVisible: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastSyncedAt: Long? = null,
)

sealed interface TradingEvent {
    data class SideSelected(val side: OrderSide) : TradingEvent
    data class QuantityChanged(val quantity: String) : TradingEvent
    data class QuickFillSelected(val fraction: Double) : TradingEvent
    data class TakeProfitChanged(val value: String) : TradingEvent
    data class StopLossChanged(val value: String) : TradingEvent
    data object ReviewOrder : TradingEvent
    data object DismissReview : TradingEvent
    data object ConfirmOrder : TradingEvent
    data object Retry : TradingEvent
    data object NavigateBack : TradingEvent
}

sealed interface TradingEffect {
    data object NavigateBack : TradingEffect
    data class ShowSnackbar(val message: String) : TradingEffect
}
