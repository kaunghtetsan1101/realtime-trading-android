package com.tradingapp.domain.usecase

import com.tradingapp.common.extension.asResult
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a [Flow] of [Result]<[List]<[Asset]>> containing only assets the user has marked as
 * favourite. Room emits automatically when [AssetRepository.toggleFavorite] is called, so the
 * Watchlist tab updates without any extra event wiring.
 */
class GetFavoritesUseCase
@Inject
constructor(private val repository: AssetRepository) {
    operator fun invoke(): Flow<Result<List<Asset>>> = repository.observeFavorites().asResult()
}
