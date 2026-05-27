package com.tradingapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tradingapp.designsystem.DarkColorScheme
import com.tradingapp.designsystem.LightColorScheme
import com.tradingapp.designsystem.TradingShapes
import com.tradingapp.designsystem.TradingTypography

@Composable
fun TradingAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = TradingTypography,
        shapes = TradingShapes,
        content = content,
    )
}
