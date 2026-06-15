package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Circular asset logo.
 *
 * When [imageUrl] is provided (sourced from CoinGecko via the metadata cache), it is
 * used directly. When null — e.g. for Position/Order rows where no [Asset] is
 * available — the component falls back to the CoinCap CDN pattern, which covers all
 * major symbols and 404s gracefully for unknowns.
 *
 * Both loading and error states render [AssetInitialsIcon] so there is no layout
 * shift when the image arrives or fails.
 */
@Composable
fun AssetIcon(symbol: String, modifier: Modifier = Modifier, imageUrl: String? = null, size: Dp = 40.dp) {
    val url = imageUrl
        ?: "https://assets.coincap.io/assets/icons/${symbol.lowercase()}@2x.png"

    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        loading = { AssetInitialsIcon(symbol = symbol, size = size) },
        error = { AssetInitialsIcon(symbol = symbol, size = size) },
    )
}

@Composable
private fun AssetInitialsIcon(symbol: String, size: Dp, modifier: Modifier = Modifier) {
    val initial = symbol.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(symbolColor(symbol)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** Stable, deterministic color derived from the symbol string. */
@Suppress("MagicNumber")
private fun symbolColor(symbol: String): Color {
    val palette = listOf(
        Color(0xFFE53935), // Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFFB8C00), // Orange
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1), // Cyan
        Color(0xFFD81B60), // Pink
        Color(0xFF3949AB), // Indigo
        Color(0xFF00897B), // Teal
        Color(0xFFF4511E), // Deep Orange
    )
    val index = symbol.fold(0) { acc, c -> acc * 31 + c.code } and Int.MAX_VALUE
    return palette[index % palette.size]
}

// --- Previews ---

@Preview(name = "Known asset — CoinGecko URL", showBackground = true)
@Preview(name = "Known asset — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssetIconKnownPreview() {
    TradingAppTheme {
        AssetIcon(
            symbol = "BTC",
            imageUrl = "https://coin-images.coingecko.com/coins/images/1/large/bitcoin.png",
            size = 48.dp,
        )
    }
}

@Preview(name = "No URL — CoinCap fallback", showBackground = true)
@Composable
private fun AssetIconCoinCapFallbackPreview() {
    TradingAppTheme {
        AssetIcon(symbol = "ETH", size = 48.dp)
    }
}

@Preview(name = "Unknown asset — initials fallback", showBackground = true)
@Composable
private fun AssetIconFallbackPreview() {
    TradingAppTheme {
        AssetIcon(symbol = "UNKNOWN", size = 48.dp)
    }
}
