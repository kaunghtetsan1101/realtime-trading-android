package com.tradingapp.data.di

import com.tradingapp.data.provider.RoomBackedAssetMetadataProvider
import com.tradingapp.data.repository.AssetMetadataRepositoryImpl
import com.tradingapp.data.repository.AssetRepositoryImpl
import com.tradingapp.data.repository.TradeRepositoryImpl
import com.tradingapp.domain.provider.AssetMetadataProvider
import com.tradingapp.domain.repository.AssetMetadataRepository
import com.tradingapp.domain.repository.AssetRepository
import com.tradingapp.domain.repository.TradeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    @Binds
    @Singleton
    abstract fun bindTradeRepository(impl: TradeRepositoryImpl): TradeRepository

    @Binds
    @Singleton
    abstract fun bindAssetMetadataProvider(impl: RoomBackedAssetMetadataProvider): AssetMetadataProvider

    @Binds
    @Singleton
    abstract fun bindAssetMetadataRepository(impl: AssetMetadataRepositoryImpl): AssetMetadataRepository
}
