package com.tradingapp.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tradingapp.marketdetail.MarketDetailScreen
import com.tradingapp.marketdetail.MarketDetailViewModel
import com.tradingapp.search.SearchScreen
import com.tradingapp.settings.SettingsScreen
import com.tradingapp.trading.PortfolioScreen
import com.tradingapp.trading.TradingScreen
import com.tradingapp.trading.TradingViewModel
import com.tradingapp.watchlist.FavoritesScreen
import com.tradingapp.watchlist.WatchlistScreen

@Composable
fun AppNavGraph() {
    val navViewModel: NavigationViewModel = hiltViewModel()
    val backStack = navViewModel.backStack

    val currentRoute = backStack.lastOrNull()
    val showBottomNav = currentRoute is RouteWatchlist ||
        currentRoute is RouteWatchlistFavorites ||
        currentRoute is RoutePortfolio

    Scaffold(
        // Inner feature screens own their TopAppBar + status-bar insets; avoid double top padding.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = currentRoute is RouteWatchlist,
                        onClick = {
                            if (currentRoute !is RouteWatchlist) {
                                backStack.clear()
                                backStack.add(RouteWatchlist)
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                        label = { Text("Market") },
                    )
                    NavigationBarItem(
                        selected = currentRoute is RouteWatchlistFavorites,
                        onClick = {
                            if (currentRoute !is RouteWatchlistFavorites) {
                                backStack.clear()
                                backStack.add(RouteWatchlistFavorites)
                            }
                        },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        label = { Text("Watchlist") },
                    )
                    NavigationBarItem(
                        selected = currentRoute is RoutePortfolio,
                        onClick = {
                            if (currentRoute !is RoutePortfolio) {
                                backStack.clear()
                                backStack.add(RoutePortfolio)
                            }
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("Portfolio") },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<RouteWatchlist> {
                    WatchlistScreen(
                        onAssetClick = { symbol -> backStack.add(RouteMarketDetail(symbol)) },
                        onSearchClick = { backStack.add(RouteSearch) },
                        onSettingsClick = { backStack.add(RouteSettings) },
                    )
                }

                entry<RouteWatchlistFavorites> {
                    FavoritesScreen(
                        onAssetClick = { symbol -> backStack.add(RouteMarketDetail(symbol)) },
                        onSettingsClick = { backStack.add(RouteSettings) },
                    )
                }

                entry<RouteMarketDetail> { key ->
                    val viewModel = hiltViewModel<MarketDetailViewModel, MarketDetailViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.symbol) },
                    )
                    MarketDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToTrade = { symbol -> backStack.add(RouteTrading(symbol)) },
                        viewModel = viewModel,
                    )
                }

                entry<RouteSearch> {
                    SearchScreen(
                        onAssetClick = { symbol -> backStack.add(RouteMarketDetail(symbol)) },
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<RouteTrading> { key ->
                    val viewModel = hiltViewModel<TradingViewModel, TradingViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.symbol) },
                    )
                    TradingScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = viewModel,
                    )
                }

                entry<RoutePortfolio> {
                    PortfolioScreen(
                        onNavigateToTrade = { symbol -> backStack.add(RouteTrading(symbol)) },
                    )
                }

                entry<RouteSettings> {
                    SettingsScreen(onNavigateBack = { backStack.removeLastOrNull() })
                }
            },
        )
    }
}
