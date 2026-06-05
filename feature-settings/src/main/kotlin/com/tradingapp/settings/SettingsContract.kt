package com.tradingapp.settings

import com.tradingapp.datastore.ThemeMode

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val verboseLoggingEnabled: Boolean = true,
    val appVersion: String = "",
)

sealed interface SettingsEvent {
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent
    data class VerboseLoggingToggled(val enabled: Boolean) : SettingsEvent
    data object NavigateBack : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
}
