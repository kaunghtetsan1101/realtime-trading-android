package com.tradingapp.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingapp.common.model.AppInfo
import com.tradingapp.datastore.AppPreferencesDataSource
import com.tradingapp.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsDataSource: AppPreferencesDataSource,
    private val appInfo: AppInfo,
) : ViewModel() {

    private val stateMutable = MutableStateFlow(SettingsState(appVersion = appInfo.versionName))
    val state: StateFlow<SettingsState> = stateMutable.asStateFlow()

    private val effectsMutable = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = effectsMutable.receiveAsFlow()

    init {
        combine(
            prefsDataSource.themeMode(),
            prefsDataSource.verboseLogging(),
        ) { themeMode, verbose ->
            themeMode to verbose
        }
            .onEach { (themeMode, verbose) ->
                stateMutable.update {
                    it.copy(themeMode = themeMode, verboseLoggingEnabled = verbose)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeModeSelected -> setThemeMode(event.mode)
            is SettingsEvent.VerboseLoggingToggled -> setVerboseLogging(event.enabled)
            SettingsEvent.NavigateBack -> sendEffect(SettingsEffect.NavigateBack)
        }
    }

    // --- Private ---

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsDataSource.setThemeMode(mode) }
    }

    private fun setVerboseLogging(enabled: Boolean) {
        viewModelScope.launch { prefsDataSource.setVerboseLogging(enabled) }
    }

    private fun sendEffect(effect: SettingsEffect) {
        viewModelScope.launch { effectsMutable.send(effect) }
    }
}
