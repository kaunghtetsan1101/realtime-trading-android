package com.tradingapp.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for the app.
 *
 * The generated file (app/src/main/baseline-prof.txt) is pre-compiled by
 * ProfileInstaller at install time, reducing cold-start latency and first-frame
 * jank on the watchlist scroll.
 *
 * Run:
 *   ./gradlew :baseline-profile:generateBaselineProfile
 *
 * Requires a connected device or emulator running API 28+.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "com.tradingapp") {
        startApp()
        waitForIdle()
        scrollWatchlist()
    }

    private fun MacrobenchmarkScope.startApp() {
        pressHome()
        startActivityAndWait()
    }

    private fun MacrobenchmarkScope.waitForIdle() {
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollWatchlist() {
        val list = device.findObject(By.scrollable(true)) ?: return
        list.scroll(Direction.DOWN, 3f)
        list.scroll(Direction.UP, 3f)
    }
}
