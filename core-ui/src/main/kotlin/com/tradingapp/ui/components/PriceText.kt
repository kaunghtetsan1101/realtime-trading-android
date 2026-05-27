package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Displays a formatted price value in a neutral colour.
 *
 * Price alone carries no directional signal, so we use [MaterialTheme.colorScheme.onSurface]
 * rather than PriceUp/Down. Pair with [PercentageBadge] to show direction.
 */
@Composable
fun PriceText(
    price: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight = FontWeight.SemiBold,
    currencySymbol: String = "",
) {
    Text(
        text = "$currencySymbol${"%.2f".format(price)}",
        style = style,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PriceTextPreview() {
    TradingAppTheme {
        PriceText(
            price = 67_500.23,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
