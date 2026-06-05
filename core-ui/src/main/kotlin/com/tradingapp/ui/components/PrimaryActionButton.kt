package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Full-width primary CTA button following the app design system.
 *
 * Uses Material3 [Button] with the theme primary colour and a minimum touch
 * target height of 48 dp (WCAG 2.5.8 — Target Size minimum).
 */
@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.xxxl),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(name = "Light — enabled", showBackground = true)
@Preview(name = "Dark — enabled", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryActionButtonEnabledPreview() {
    TradingAppTheme { PrimaryActionButton(text = "Buy BTC", onClick = {}) }
}

@Preview(name = "Light — disabled", showBackground = true)
@Composable
private fun PrimaryActionButtonDisabledPreview() {
    TradingAppTheme { PrimaryActionButton(text = "Buy BTC", onClick = {}, enabled = false) }
}
