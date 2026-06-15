package com.tradingapp.domain.usecase

import com.tradingapp.domain.model.AssetMetadata
import com.tradingapp.domain.repository.AssetMetadataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssetMetadataUseCase
@Inject
constructor(private val repository: AssetMetadataRepository) {
    operator fun invoke(baseSymbol: String): Flow<AssetMetadata?> = repository.observeMetadata(baseSymbol)
}
