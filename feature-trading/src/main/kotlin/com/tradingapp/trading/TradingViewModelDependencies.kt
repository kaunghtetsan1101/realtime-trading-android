package com.tradingapp.trading

import com.tradingapp.domain.repository.TradeRepository
import com.tradingapp.domain.usecase.GetAssetDetailUseCase
import com.tradingapp.domain.usecase.ObserveNetworkStatusUseCase
import com.tradingapp.domain.usecase.ObservePriceTicksUseCase
import com.tradingapp.domain.usecase.PlaceOrderUseCase
import com.tradingapp.domain.usecase.ValidateOrderUseCase
import com.tradingapp.domain.usecase.ValidateTakeProfitStopLossUseCase
import javax.inject.Inject

class TradingViewModelDependencies @Inject constructor(
    val getAssetDetail: GetAssetDetailUseCase,
    val observePriceTicks: ObservePriceTicksUseCase,
    val validateOrder: ValidateOrderUseCase,
    val validateTpSl: ValidateTakeProfitStopLossUseCase,
    val placeOrder: PlaceOrderUseCase,
    val tradeRepository: TradeRepository,
    val observeNetworkStatus: ObserveNetworkStatusUseCase,
)
