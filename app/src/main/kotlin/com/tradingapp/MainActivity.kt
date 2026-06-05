package com.tradingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.datastore.AppPreferencesDataSource
import com.tradingapp.datastore.ThemeMode
import com.tradingapp.navigation.AppNavGraph
import com.tradingapp.ui.theme.TradingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsDataSource: AppPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by prefsDataSource.themeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            TradingAppTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
    }
}
