package com.tradingapp.domain.usecase

import com.tradingapp.domain.repository.AssetMetadataRepository
import javax.inject.Inject

class SyncAssetMetadataUseCase
@Inject
constructor(private val repository: AssetMetadataRepository) {
    suspend operator fun invoke() = repository.syncIfStale()
}
