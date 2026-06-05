package com.tradingapp.watchlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.domain.model.Asset
import com.tradingapp.ui.components.AssetRow
import com.tradingapp.ui.components.EmptyState
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.components.OfflineBanner
import com.tradingapp.ui.theme.TradingAppTheme

@Composable
fun WatchlistScreen(
    onAssetClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPortfolioClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WatchlistEffect.NavigateToDetail -> onAssetClick(effect.symbol)
                is WatchlistEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    WatchlistContent(
        state = state,
        snackbarHost = snackbarHost,
        onEvent = viewModel::onEvent,
        onSearchClick = onSearchClick,
        onPortfolioClick = onPortfolioClick,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistContent(
    state: WatchlistState,
    snackbarHost: SnackbarHostState,
    onEvent: (WatchlistEvent) -> Unit,
    onSearchClick: () -> Unit,
    onPortfolioClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Market Watch", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onPortfolioClick) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Open portfolio")
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Search markets")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings")
                        }
                    },
                )
                OfflineBanner(
                    isOffline = state.isOffline,
                    lastUpdatedMs = state.assets.firstOrNull()?.lastUpdated,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        when {
            state.isLoading && state.assets.isEmpty() -> LoadingIndicator(Modifier.padding(padding))
            state.error != null && state.assets.isEmpty() -> ErrorState(
                message = state.error,
                onRetry = { onEvent(WatchlistEvent.Refresh) },
                modifier = Modifier.padding(padding),
            )
            state.assets.isEmpty() -> EmptyState(
                title = "No assets",
                subtitle = "Pull to refresh or check your connection.",
                modifier = Modifier.padding(padding),
            )
            else -> AssetList(
                assets = state.assets,
                onEvent = onEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun AssetList(assets: List<Asset>, onEvent: (WatchlistEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(assets, key = { it.symbol }) { asset ->
            AssetRow(
                symbol = asset.symbol,
                name = asset.name,
                price = asset.currentPrice,
                changePercent = asset.priceChangePct24h,
                isFavorite = asset.isFavorite,
                onRowClick = { onEvent(WatchlistEvent.AssetClicked(asset.symbol)) },
                onFavoriteClick = { onEvent(WatchlistEvent.ToggleFavorite(asset.symbol, !asset.isFavorite)) },
            )
            HorizontalDivider()
        }
    }
}

// --- Previews ---

private fun fakeAsset(symbol: String, price: Double, pct: Double, fav: Boolean = false) = Asset(
    symbol = symbol,
    name = symbol,
    currentPrice = price,
    priceChange24h = price * pct / 100,
    priceChangePct24h = pct,
    marketCap = 1_000_000_000.0,
    volume24h = 50_000_000.0,
    logoUrl = null,
    isFavorite = fav,
    lastUpdated = System.currentTimeMillis(),
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WatchlistPreview() {
    TradingAppTheme {
        WatchlistContent(
            state = WatchlistState(
                isLoading = false,
                assets = listOf(
                    fakeAsset("BTC", 67_500.0, 2.34, fav = true),
                    fakeAsset("ETH", 3_450.0, -1.12),
                    fakeAsset("SOL", 175.0, 5.67),
                ),
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onSearchClick = {},
            onPortfolioClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "Light — loading", showBackground = true)
@Composable
private fun WatchlistLoadingPreview() {
    TradingAppTheme {
        WatchlistContent(WatchlistState(), remember { SnackbarHostState() }, {}, {}, {}, {})
    }
}

@Preview(name = "Light — error", showBackground = true)
@Composable
private fun WatchlistErrorPreview() {
    TradingAppTheme {
        WatchlistContent(
            WatchlistState(isLoading = false, error = "No internet connection. Showing cached data."),
            remember { SnackbarHostState() },
            {},
            {},
            {},
            {},
        )
    }
}
