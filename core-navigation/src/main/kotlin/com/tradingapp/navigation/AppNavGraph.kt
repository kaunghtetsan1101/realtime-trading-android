package com.tradingapp.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tradingapp.marketdetail.MarketDetailScreen
import com.tradingapp.marketdetail.MarketDetailViewModel
import com.tradingapp.search.SearchScreen
import com.tradingapp.watchlist.WatchlistScreen

@Composable
fun AppNavGraph() {
    // ViewModel is retained across configuration changes; back stack survives rotation.
    val navViewModel: NavigationViewModel = hiltViewModel()
    val backStack = navViewModel.backStack

    NavDisplay(
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
                )
            }

            entry<RouteMarketDetail> { key ->
                val viewModel = hiltViewModel<MarketDetailViewModel, MarketDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key.symbol) },
                )
                MarketDetailScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    viewModel = viewModel,
                )
            }

            entry<RouteSearch> {
                SearchScreen(
                    onAssetClick = { symbol -> backStack.add(RouteMarketDetail(symbol)) },
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            // Placeholder — implemented in a future milestone
            entry<RouteSettings> { }
        },
    )
}
