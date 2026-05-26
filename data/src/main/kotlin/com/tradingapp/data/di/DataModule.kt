package com.tradingapp.data.di

import com.tradingapp.data.repository.AssetRepositoryImpl
import com.tradingapp.domain.repository.AssetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WsUrl

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    companion object {
        /**
         * Assembles the Binance combined-stream WebSocket URL from [AssetRepositoryImpl.TRACKED_SYMBOLS].
         *
         * Example: wss://stream.binance.com:9443/stream?streams=btcusdt@miniTicker/ethusdt@miniTicker/...
         *
         * Tradeoff: building the URL here (rather than inside the repository) keeps
         * [AssetRepositoryImpl] free of URL-construction logic and lets us swap the
         * endpoint without touching repository code.
         */
        @Provides
        @WsUrl
        fun provideWsUrl(): String {
            val streams = AssetRepositoryImpl.TRACKED_SYMBOLS
                .joinToString("/") { "${it.lowercase()}@miniTicker" }
            return "wss://stream.binance.com:9443/stream?streams=$streams"
        }
    }
}
