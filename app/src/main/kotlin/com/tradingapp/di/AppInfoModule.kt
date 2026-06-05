package com.tradingapp.di

import com.tradingapp.BuildConfig
import com.tradingapp.common.model.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {

    @Provides
    @Singleton
    fun provideAppInfo(): AppInfo = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
    )
}
