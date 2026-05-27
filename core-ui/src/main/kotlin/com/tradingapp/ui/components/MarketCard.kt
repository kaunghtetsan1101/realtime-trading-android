package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Card-style asset tile for grid or featured-asset layouts.
 *
 * Wraps [PriceText] and [PercentageBadge] inside a Material3 [Card].
 * Intended for horizontal carousels or grid screens in future milestones.
 */
@Composable
fun MarketCard(
    symbol: String,
    name: String,
    price: Double,
    changePercent: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                PercentageBadge(changePercent = changePercent)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PriceText(
                price = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MarketCardPreview() {
    TradingAppTheme {
        MarketCard(symbol = "BTC", name = "Bitcoin", price = 67_500.0, changePercent = 2.34, onClick = {})
    }
}

@Preview(name = "Light — negative", showBackground = true)
@Composable
private fun MarketCardDownPreview() {
    TradingAppTheme {
        MarketCard(symbol = "ETH", name = "Ethereum", price = 3_450.0, changePercent = -1.12, onClick = {})
    }
}
