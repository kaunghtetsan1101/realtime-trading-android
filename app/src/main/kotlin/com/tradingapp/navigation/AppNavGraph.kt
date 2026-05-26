package com.tradingapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tradingapp.marketdetail.MarketDetailScreen
import com.tradingapp.marketdetail.MarketDetailViewModel
import com.tradingapp.watchlist.WatchlistScreen

@Composable
fun AppNavGraph() {
    val backStack = remember { mutableStateListOf<Any>(RouteWatchlist) }

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
                    onAssetClick = { symbol ->
                        backStack.add(RouteMarketDetail(symbol))
                    },
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

            // Placeholder entries — implemented in future milestones
            entry<RouteSearch> { }
            entry<RouteSettings> { }
        },
    )
}
