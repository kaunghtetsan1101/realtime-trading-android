package com.tradingapp.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.domain.model.Asset
import com.tradingapp.ui.components.EmptyState
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.theme.PriceDown
import com.tradingapp.ui.theme.PriceUp
import com.tradingapp.ui.theme.TradingAppTheme
import java.util.Locale

@Composable
fun WatchlistScreen(
    onAssetClick: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WatchlistEffect.NavigateToDetail -> onAssetClick(effect.symbol)
                is WatchlistEffect.ShowSnackbar     -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    WatchlistContent(
        state        = state,
        snackbarHost = snackbarHost,
        onEvent      = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistContent(
    state: WatchlistState,
    snackbarHost: SnackbarHostState,
    onEvent: (WatchlistEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Market Watch", fontWeight = FontWeight.SemiBold) })
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
                title    = "No assets",
                subtitle = "Pull to refresh or check your connection.",
                modifier = Modifier.padding(padding),
            )
            else -> AssetList(
                assets   = state.assets,
                onEvent  = onEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun AssetList(
    assets: List<Asset>,
    onEvent: (WatchlistEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(assets, key = { it.symbol }) { asset ->
            AssetRow(
                asset    = asset,
                onClick  = { onEvent(WatchlistEvent.AssetClicked(asset.symbol)) },
                onFavClick = {
                    onEvent(WatchlistEvent.ToggleFavorite(asset.symbol, !asset.isFavorite))
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun AssetRow(
    asset: Asset,
    onClick: () -> Unit,
    onFavClick: () -> Unit,
) {
    val priceColor = if (asset.isUp) PriceUp else PriceDown
    val trendIcon  = if (asset.isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val pctSign    = if (asset.isUp) "+" else ""
    val pctLabel   = "$pctSign${"%.2f".format(asset.priceChangePct24h)}%"

    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Symbol + name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = asset.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = asset.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        // Price + change
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = "%.2f".format(asset.currentPrice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = trendIcon,
                    contentDescription = null,
                    tint               = priceColor,
                    modifier           = Modifier.padding(end = 2.dp),
                )
                Text(
                    text  = pctLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = priceColor,
                )
            }
        }

        // Favorite
        IconButton(onClick = onFavClick) {
            Icon(
                imageVector = if (asset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (asset.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (asset.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// --- Previews ---

private fun fakeAsset(symbol: String, price: Double, pct: Double, fav: Boolean = false) = Asset(
    symbol            = symbol,
    name              = symbol,
    currentPrice      = price,
    priceChange24h    = price * pct / 100,
    priceChangePct24h = pct,
    marketCap         = 1_000_000_000.0,
    volume24h         = 50_000_000.0,
    logoUrl           = null,
    isFavorite        = fav,
    lastUpdated       = System.currentTimeMillis(),
)

@Preview(showBackground = true)
@Composable
private fun WatchlistPreview() {
    TradingAppTheme {
        WatchlistContent(
            state = WatchlistState(
                isLoading = false,
                assets    = listOf(
                    fakeAsset("BTC",  67_500.0,  2.34, fav = true),
                    fakeAsset("ETH",   3_450.0, -1.12),
                    fakeAsset("SOL",     175.0,  5.67),
                ),
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent      = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistLoadingPreview() {
    TradingAppTheme { WatchlistContent(WatchlistState(), remember { SnackbarHostState() }, {}) }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistErrorPreview() {
    TradingAppTheme {
        WatchlistContent(
            WatchlistState(isLoading = false, error = "Network unavailable"),
            remember { SnackbarHostState() },
            {},
        )
    }
}
