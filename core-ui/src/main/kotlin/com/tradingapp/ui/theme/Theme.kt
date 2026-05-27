package com.tradingapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette
private val Green500 = Color(0xFF00C853)
private val Green700 = Color(0xFF009624)
private val Red500 = Color(0xFFD50000)
private val Surface = Color(0xFF121212)

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Color.Black,
    primaryContainer = Green700,
    secondary = Color(0xFF03DAC5),
    background = Color(0xFF0D0D0D),
    surface = Surface,
    error = Red500,
)

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFF018786),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    error = Red500,
)

// Semantic aliases for price changes
val PriceUp = Green500
val PriceDown = Red500

@Composable
fun TradingAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
