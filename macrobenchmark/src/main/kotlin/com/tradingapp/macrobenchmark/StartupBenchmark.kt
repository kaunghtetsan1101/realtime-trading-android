package com.tradingapp.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time with no pre-compilation (worst case) and with a
 * Baseline Profile applied (realistic install scenario).
 *
 * Run on a physical device or rooted emulator (requires API 29+):
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
 *
 * Results are written to the device and reported in Android Studio's
 * Benchmark tab.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartNoCompilation() = startup(CompilationMode.None())

    @Test
    fun coldStartBaselineProfile() = startup(CompilationMode.Partial())

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = "com.tradingapp",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
