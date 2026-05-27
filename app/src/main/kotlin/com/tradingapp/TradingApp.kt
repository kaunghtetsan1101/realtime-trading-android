package com.tradingapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TradingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // DebugTree tags each log with the calling class name and line number automatically.
            // No tree is planted in release builds — zero logging overhead in production.
            Timber.plant(Timber.DebugTree())
        }
    }
}
