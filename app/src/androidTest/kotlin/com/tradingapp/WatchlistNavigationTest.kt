package com.tradingapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke test: launch app → watchlist loads → tap BTC row →
 * detail screen appears → press back → watchlist visible again.
 *
 * Requires a connected device or emulator with internet access (Binance public API).
 * Run with: ./gradlew :app:connectedDebugAndroidTest
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WatchlistNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_watchlistLoads_tapAssetRow_detailVisible_backReturnsToWatchlist() {
        // Watchlist screen is visible on launch
        composeTestRule.onNodeWithText("Market Watch").assertIsDisplayed()

        // Wait for the asset list to populate (Binance REST sync + Room write)
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("BTC").fetchSemanticsNodes().isNotEmpty()
        }

        // Tap the BTC row — the clickable Row merges child semantics so this hits onRowClick
        composeTestRule.onNodeWithText("BTC").performClick()

        // Detail screen is visible — identified by its back-navigation button
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Navigate back")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()

        // Navigate back to watchlist
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // Watchlist screen is visible again
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Market Watch").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Market Watch").assertIsDisplayed()
    }
}
