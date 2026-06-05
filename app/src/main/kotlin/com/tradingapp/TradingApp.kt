package com.tradingapp

import android.app.Application
import com.tradingapp.BuildConfig
import com.tradingapp.datastore.AppPreferencesDataSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TradingApp : Application() {

    @Inject
    lateinit var prefsDataSource: AppPreferencesDataSource

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            val verboseEnabled = runBlocking { prefsDataSource.verboseLogging().first() }
            if (verboseEnabled) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}
