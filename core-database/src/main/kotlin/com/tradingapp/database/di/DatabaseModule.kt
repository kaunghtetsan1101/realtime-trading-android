package com.tradingapp.database.di

import android.content.Context
import androidx.room.Room
import com.tradingapp.database.TradingDatabase
import com.tradingapp.database.dao.AssetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TradingDatabase = Room
        .databaseBuilder(
            context,
            TradingDatabase::class.java,
            "trading.db",
        ).addMigrations(TradingDatabase.MIGRATION_1_2)
        .build()

    @Provides
    fun provideAssetDao(db: TradingDatabase): AssetDao = db.assetDao()
}
