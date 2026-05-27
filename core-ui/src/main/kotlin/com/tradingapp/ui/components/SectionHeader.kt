package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Section title row with an optional trailing action label.
 *
 * Used in MarketDetail stats section and reusable for any future section
 * header (e.g. "Recent Trades", "Top Movers").
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trailingText != null && onTrailingClick != null) {
            TextButton(onClick = onTrailingClick) {
                Text(text = trailingText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(name = "Light — title only", showBackground = true)
@Preview(name = "Dark — title only", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SectionHeaderSimplePreview() {
    TradingAppTheme { SectionHeader(title = "Stats") }
}

@Preview(name = "Light — with trailing action", showBackground = true)
@Composable
private fun SectionHeaderWithActionPreview() {
    TradingAppTheme {
        SectionHeader(title = "Recent Trades", trailingText = "See all", onTrailingClick = {})
    }
}
