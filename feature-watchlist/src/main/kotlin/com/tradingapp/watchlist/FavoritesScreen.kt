package com.tradingapp.watchlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
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
fun FavoritesScreen(
    onAssetClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
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

    FavoritesContent(
        state = state,
        snackbarHost = snackbarHost,
        onEvent = viewModel::onEvent,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesContent(
    state: WatchlistState,
    snackbarHost: SnackbarHostState,
    onEvent: (WatchlistEvent) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Watchlist", fontWeight = FontWeight.SemiBold) },
                    actions = {
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
                icon = Icons.Default.Bookmark,
                title = "No saved assets",
                subtitle = "Tap ★ on any asset in Market to add it here.",
                modifier = Modifier.padding(padding),
            )
            else -> FavoritesList(
                assets = state.assets,
                onEvent = onEvent,
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
        }
    }
}

@Composable
private fun FavoritesList(assets: List<Asset>, onEvent: (WatchlistEvent) -> Unit, modifier: Modifier = Modifier) {
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
                logoUrl = asset.logoUrl,
            )
            HorizontalDivider()
        }
    }
}

// --- Previews ---

private fun fakeAsset(symbol: String, price: Double, pct: Double) = Asset(
    symbol = symbol,
    name = symbol,
    currentPrice = price,
    priceChange24h = price * pct / 100,
    priceChangePct24h = pct,
    marketCap = 1_000_000_000.0,
    volume24h = 50_000_000.0,
    logoUrl = null,
    isFavorite = true,
    lastUpdated = System.currentTimeMillis(),
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FavoritesPreview() {
    TradingAppTheme {
        FavoritesContent(
            state = WatchlistState(
                isLoading = false,
                assets = listOf(
                    fakeAsset("BTC", 67_500.0, 2.34),
                    fakeAsset("ETH", 3_450.0, -1.12),
                ),
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "Light — empty", showBackground = true)
@Composable
private fun FavoritesEmptyPreview() {
    TradingAppTheme {
        FavoritesContent(
            state = WatchlistState(isLoading = false),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onSettingsClick = {},
        )
    }
}
