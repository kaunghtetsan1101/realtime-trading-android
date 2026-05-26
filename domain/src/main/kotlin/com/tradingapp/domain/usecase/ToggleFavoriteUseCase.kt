package com.tradingapp.domain.usecase

import com.tradingapp.domain.repository.AssetRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(symbol: String, isFavorite: Boolean) =
        repository.toggleFavorite(symbol, isFavorite)
}
