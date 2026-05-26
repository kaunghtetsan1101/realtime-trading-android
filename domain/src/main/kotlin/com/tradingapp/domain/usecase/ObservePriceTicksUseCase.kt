package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.PriceTick
import com.tradingapp.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePriceTicksUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    operator fun invoke(symbol: String): Flow<PriceTick> =
        repository.observePriceTicks(symbol)
}
