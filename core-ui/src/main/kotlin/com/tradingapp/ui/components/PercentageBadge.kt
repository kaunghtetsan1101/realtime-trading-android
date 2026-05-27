package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.designsystem.PriceDown
import com.tradingapp.designsystem.PriceUp
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Colour-coded percentage change badge with a trending icon.
 *
 * Positive values use [PriceUp] green; negative use [PriceDown] red.
 * The row merges into a single semantics node so screen readers announce
 * it as one value (e.g. "Up +2.34%").
 */
@Composable
fun PercentageBadge(
    changePercent: Double,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val isUp = changePercent >= 0.0
    val color = if (isUp) PriceUp else PriceDown
    val sign = if (isUp) "+" else ""
    val label = "$sign${"%.2f".format(changePercent)}%"
    val a11yLabel = if (isUp) "Up $label" else "Down $label"
    val icon = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = a11yLabel },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(end = Spacing.xxs),
        )
        Text(
            text = label,
            style = textStyle,
            fontWeight = fontWeight,
            color = color,
        )
    }
}

@Preview(name = "Light — positive", showBackground = true)
@Preview(name = "Dark — positive", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PercentageBadgeUpPreview() {
    TradingAppTheme { PercentageBadge(changePercent = 2.34) }
}

@Preview(name = "Light — negative", showBackground = true)
@Preview(name = "Dark — negative", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PercentageBadgeDownPreview() {
    TradingAppTheme { PercentageBadge(changePercent = -1.12) }
}
