package com.tradingapp.ui.components

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
import androidx.compose.ui.unit.dp

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 8.dp),
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

private fun formatAge(lastUpdatedMs: Long?): String {
    if (lastUpdatedMs == null || lastUpdatedMs == 0L) return "unknown"
    val ageMs = System.currentTimeMillis() - lastUpdatedMs
    return when {
        ageMs < 60_000L -> "just now"
        else -> "${ageMs / 60_000L} min ago"
    }
}
