package com.tradingapp.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.datastore.ThemeMode
import com.tradingapp.designsystem.Spacing
import com.tradingapp.ui.components.SectionHeader
import com.tradingapp.ui.theme.TradingAppTheme

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    SettingsContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // --- Appearance ---
            item {
                SectionHeader(
                    title = "Appearance",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            item {
                ThemeModeSelector(
                    selectedMode = state.themeMode,
                    onModeSelected = { onEvent(SettingsEvent.ThemeModeSelected(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(top = Spacing.sm)) }

            // --- Developer Options ---
            item {
                SectionHeader(
                    title = "Developer Options",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Verbose Logging") },
                    supportingContent = {
                        Text(
                            "Log debug events to logcat. Restart app to apply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.verboseLoggingEnabled,
                            onCheckedChange = { onEvent(SettingsEvent.VerboseLoggingToggled(it)) },
                        )
                    },
                )
            }
            item { HorizontalDivider() }

            // --- About ---
            item {
                SectionHeader(
                    title = "About",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    trailingContent = {
                        Text(
                            state.appVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Realtime Trading") },
                    supportingContent = {
                        Text(
                            "Mock market watch and trading app · Portfolio project",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            item { HorizontalDivider() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
    Column(modifier = modifier) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                )
            }
        }
    }
}

// --- Previews ---

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    TradingAppTheme {
        SettingsContent(
            state = SettingsState(
                themeMode = ThemeMode.SYSTEM,
                verboseLoggingEnabled = true,
                appVersion = "1.0.0",
            ),
            onEvent = {},
        )
    }
}
