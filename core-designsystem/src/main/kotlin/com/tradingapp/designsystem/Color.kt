package com.tradingapp.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Raw palette ───────────────────────────────────────────────────────────────
internal val Green500 = Color(0xFF00C853) // primary brand green
internal val Green700 = Color(0xFF009624) // darker green for light theme
internal val Red500 = Color(0xFFD50000) // error / price-down

// ── Semantic tokens ───────────────────────────────────────────────────────────

/** Tint for a positive price change. */
val PriceUp: Color = Green500

/** Tint for a negative price change. */
val PriceDown: Color = Red500

// ── Material3 colour schemes ──────────────────────────────────────────────────

val DarkColorScheme =
    darkColorScheme(
        primary = Green500,
        onPrimary = Color.Black,
        primaryContainer = Green700,
        secondary = Color(0xFF03DAC5),
        background = Color(0xFF0D0D0D),
        surface = Color(0xFF121212),
        error = Red500,
    )

val LightColorScheme =
    lightColorScheme(
        primary = Green700,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB9F6CA),
        secondary = Color(0xFF018786),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        error = Red500,
    )
