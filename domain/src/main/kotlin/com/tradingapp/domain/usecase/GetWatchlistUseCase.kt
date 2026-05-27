package com.tradingapp.domain.usecase

import com.tradingapp.common.extension.asResult
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a [Flow] of [Result]<[List]<[Asset]>> representing the full market watchlist.
 *
 * The [asResult] operator wraps emissions so the UI receives Loading → Success / Error
 * without knowing about exceptions.
 */
class GetWatchlistUseCase
@Inject
constructor(private val repository: AssetRepository) {
    operator fun invoke(): Flow<Result<List<Asset>>> = repository.observeAssets().asResult()
}
