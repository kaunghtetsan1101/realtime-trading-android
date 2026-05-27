package com.tradingapp.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.theme.TradingAppTheme

/**
 * Animated banner shown when the device is offline.
 *
 * Displays a WiFi-off icon and a "Cached · X min ago" label derived from [lastUpdatedMs].
 * Slides in/out with [AnimatedVisibility] so the layout does not jump.
 */
@Composable
fun OfflineBanner(isOffline: Boolean, lastUpdatedMs: Long?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = Spacing.sm),
                )
                Text(
                    text = "Cached · ${formatAge(lastUpdatedMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

private const val ONE_MINUTE_MS = 60_000L

private fun formatAge(lastUpdatedMs: Long?): String {
    if (lastUpdatedMs == null || lastUpdatedMs == 0L) return "unknown"
    val ageMs = System.currentTimeMillis() - lastUpdatedMs
    return when {
        ageMs < ONE_MINUTE_MS -> "just now"
        else -> "${ageMs / ONE_MINUTE_MS} min ago"
    }
}

@Preview(name = "Light — offline", showBackground = true)
@Preview(name = "Dark — offline", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflineBannerPreview() {
    TradingAppTheme {
        // Simulate 5 min ago cache
        OfflineBanner(isOffline = true, lastUpdatedMs = System.currentTimeMillis() - 5 * ONE_MINUTE_MS)
    }
}
