package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Single asset list row — shared by Watchlist and Search screens.
 *
 * Extracted from duplicated local implementations in each feature. All colour
 * and icon logic is encapsulated here via [PriceText] and [PercentageBadge],
 * so callers only pass plain data and callbacks.
 *
 * Accessibility: the favourite [IconButton] carries an explicit content
 * description so screen readers announce the toggle action with the asset symbol.
 */
@Composable
fun AssetRow(
    symbol: String,
    name: String,
    price: Double,
    changePercent: Double,
    isFavorite: Boolean,
    onRowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        AssetIcon(symbol = symbol, imageUrl = logoUrl, size = 40.dp)

        // Symbol + name
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Price + change badge
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = Spacing.sm),
        ) {
            PriceText(price = price)
            PercentageBadge(changePercent = changePercent)
        }

        // Favourite toggle
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove $symbol from favorites" else "Add $symbol to favorites",
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Preview(name = "Light — favorite", showBackground = true)
@Preview(name = "Dark — favorite", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssetRowFavPreview() {
    TradingAppTheme {
        AssetRow(
            symbol = "BTC",
            name = "Bitcoin",
            price = 67_500.0,
            changePercent = 2.34,
            isFavorite = true,
            onRowClick = {},
            onFavoriteClick = {},
        )
    }
}

@Preview(name = "Light — not favorite, negative", showBackground = true)
@Preview(name = "Dark — not favorite, negative", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssetRowDownPreview() {
    TradingAppTheme {
        AssetRow(
            symbol = "ETH",
            name = "Ethereum",
            price = 3_450.0,
            changePercent = -1.12,
            isFavorite = false,
            onRowClick = {},
            onFavoriteClick = {},
        )
    }
}
