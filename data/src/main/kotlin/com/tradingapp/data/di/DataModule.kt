package com.tradingapp.data.di

import com.tradingapp.data.repository.AssetRepositoryImpl
import com.tradingapp.domain.repository.AssetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// The WebSocket URL is now built dynamically inside AssetRepositoryImpl based on
// symbols discovered from Binance — no DI-time constant is needed.

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository
}
