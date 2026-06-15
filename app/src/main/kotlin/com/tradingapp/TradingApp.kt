package com.tradingapp

import android.app.Application
import com.tradingapp.BuildConfig
import com.tradingapp.datastore.AppPreferencesDataSource
import com.tradingapp.domain.usecase.SyncAssetMetadataUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TradingApp : Application() {

    @Inject lateinit var prefsDataSource: AppPreferencesDataSource
    @Inject lateinit var syncAssetMetadata: SyncAssetMetadataUseCase

    // Survive the full app lifetime — not tied to any Activity or ViewModel scope.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            val verboseEnabled = runBlocking { prefsDataSource.verboseLogging().first() }
            if (verboseEnabled) {
                Timber.plant(Timber.DebugTree())
            }
        }
        // Warm the in-memory metadata cache from Room and fetch fresh CoinGecko data
        // if the local copy is stale. Runs fully in the background — never blocks the UI.
        appScope.launch { syncAssetMetadata() }
    }
}
