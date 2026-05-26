package com.tradingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tradingapp.watchlist.WatchlistScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Routes.Watchlist.route,
    ) {
        composable(Routes.Watchlist.route) {
            WatchlistScreen(
                onAssetClick = { symbol ->
                    navController.navigate(Routes.MarketDetail.createRoute(symbol))
                },
            )
        }

        // Placeholder routes — implemented in future milestones
        composable(Routes.MarketDetail.route) { /* feature-market-detail */ }
        composable(Routes.Search.route)       { /* feature-search */ }
        composable(Routes.Settings.route)     { /* feature-settings */ }
    }
}
