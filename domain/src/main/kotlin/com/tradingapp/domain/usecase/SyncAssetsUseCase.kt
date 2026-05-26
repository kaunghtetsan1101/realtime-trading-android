package com.tradingapp.domain.usecase

import com.tradingapp.domain.repository.AssetRepository
import javax.inject.Inject

class SyncAssetsUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.syncAssets()
}
