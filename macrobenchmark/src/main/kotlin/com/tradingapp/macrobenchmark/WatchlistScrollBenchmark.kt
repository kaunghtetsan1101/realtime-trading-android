package com.tradingapp.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures watchlist scroll frame timing — P50/P90/P99 frame durations.
 *
 * The `coldStartBaselineProfile` variant shows the benefit of profile-guided
 * AOT compilation: the JIT has fewer classes to compile on-the-fly during
 * the first scroll, reducing jank.
 */
@RunWith(AndroidJUnit4::class)
class WatchlistScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollNoCompilation() = scroll(CompilationMode.None())

    @Test
    fun scrollBaselineProfile() = scroll(CompilationMode.Partial())

    private fun scroll(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = "com.tradingapp",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        device.findObject(By.scrollable(true))?.let { list ->
            repeat(3) { list.scroll(Direction.DOWN, 1f) }
        }
    }
}
