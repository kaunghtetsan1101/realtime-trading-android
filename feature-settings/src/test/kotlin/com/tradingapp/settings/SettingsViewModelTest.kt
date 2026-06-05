package com.tradingapp.settings

import app.cash.turbine.test
import com.tradingapp.common.model.AppInfo
import com.tradingapp.datastore.AppPreferencesDataSource
import com.tradingapp.datastore.ThemeMode
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val prefsDataSource = mockk<AppPreferencesDataSource>(relaxed = true)
    private val appInfo = AppInfo(versionName = "1.0.0", versionCode = 1)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { prefsDataSource.themeMode() } returns flowOf(ThemeMode.SYSTEM)
        every { prefsDataSource.verboseLogging() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has app version from AppInfo`() = runTest {
        viewModel = buildViewModel()
        assertEquals("1.0.0", viewModel.state.value.appVersion)
    }

    @Test
    fun `state reflects stored theme mode`() = runTest {
        every { prefsDataSource.themeMode() } returns flowOf(ThemeMode.DARK)
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
    }

    @Test
    fun `state reflects stored verbose logging`() = runTest {
        every { prefsDataSource.verboseLogging() } returns flowOf(false)
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.verboseLoggingEnabled)
    }

    @Test
    fun `ThemeModeSelected event calls setThemeMode on data source`() = runTest {
        viewModel = buildViewModel()

        viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.LIGHT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { prefsDataSource.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `VerboseLoggingToggled event calls setVerboseLogging on data source`() = runTest {
        viewModel = buildViewModel()

        viewModel.onEvent(SettingsEvent.VerboseLoggingToggled(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { prefsDataSource.setVerboseLogging(false) }
    }

    @Test
    fun `NavigateBack event sends NavigateBack effect`() = runTest {
        viewModel = buildViewModel()
        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(SettingsEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `theme mode updates reactively when data source emits`() = runTest {
        val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
        every { prefsDataSource.themeMode() } returns themeModeFlow

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.state.value.themeMode)

        themeModeFlow.value = ThemeMode.DARK
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
    }

    @Test
    fun `verbose logging updates reactively when data source emits`() = runTest {
        val loggingFlow = MutableStateFlow(true)
        every { prefsDataSource.verboseLogging() } returns loggingFlow

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.verboseLoggingEnabled)

        loggingFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.verboseLoggingEnabled)
    }

    // --- Helpers ---

    private fun buildViewModel() = SettingsViewModel(prefsDataSource, appInfo)
}
