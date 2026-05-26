package com.tradingapp.domain.usecase

import com.tradingapp.common.extension.asResult
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssetDetailUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    operator fun invoke(symbol: String): Flow<Result<Asset?>> =
        repository.observeAsset(symbol).asResult()
}
