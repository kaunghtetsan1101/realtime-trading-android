package com.tradingapp.marketdetail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.domain.model.Asset
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.components.OfflineBanner
import com.tradingapp.ui.theme.PriceDown
import com.tradingapp.ui.theme.PriceUp
import com.tradingapp.ui.theme.TradingAppTheme

// viewModel is supplied by AppNavGraph via hiltViewModel(creationCallback = ...) — not a default param
// because the Factory needs the symbol from the Nav3 route key.
@Composable
fun MarketDetailScreen(onNavigateBack: () -> Unit, viewModel: MarketDetailViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MarketDetailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    MarketDetailContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketDetailContent(state: MarketDetailState, onEvent: (MarketDetailEvent) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val title = state.asset?.let { "${it.symbol} / ${it.name}" } ?: "Detail"

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MarketDetailEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.asset == null -> LoadingIndicator(Modifier.padding(padding))
            state.error != null && state.asset == null -> ErrorState(
                message = state.error,
                onRetry = { onEvent(MarketDetailEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            state.asset != null -> DetailBody(
                asset = state.asset,
                isOffline = state.isOffline,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DetailBody(asset: Asset, isOffline: Boolean, modifier: Modifier = Modifier) {
    val darkTheme = isSystemInDarkTheme()
    val priceColor = if (asset.isUp) PriceUp else PriceDown
    val pctSign = if (asset.isUp) "+" else ""
    val pctLabel = "$pctSign${"%.2f".format(asset.priceChangePct24h)}%"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // --- Price header ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "%.2f".format(asset.currentPrice),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pctLabel,
                style = MaterialTheme.typography.titleMedium,
                color = priceColor,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // --- TradingView chart ---
        TradingViewChart(
            symbol = asset.symbol,
            darkTheme = darkTheme,
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
        )

        OfflineBanner(
            isOffline = isOffline,
            lastUpdatedMs = asset.lastUpdated,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Stats grid ---
        StatsSection(asset = asset)
    }
}

@Composable
private fun StatsSection(asset: Asset) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Stats",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        StatRow(label = "24h High", value = "${"%.2f".format(asset.high24h)}")
        StatRow(label = "24h Low", value = "${"%.2f".format(asset.low24h)}")
        StatRow(label = "Volume", value = formatLargeNumber(asset.volume24h))
        StatRow(label = "Mkt Cap", value = formatLargeNumber(asset.marketCap))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatLargeNumber(value: Double): String = when {
    value >= 1_000_000_000.0 -> "${"%.2f".format(value / 1_000_000_000.0)}B"
    value >= 1_000_000.0 -> "${"%.2f".format(value / 1_000_000.0)}M"
    value >= 1_000.0 -> "${"%.2f".format(value / 1_000.0)}K"
    else -> "%.2f".format(value)
}

// --- Previews ---

private fun fakeAsset() = Asset(
    symbol = "BTC",
    name = "Bitcoin",
    currentPrice = 67_500.0,
    priceChange24h = 1_575.0,
    priceChangePct24h = 2.34,
    high24h = 68_200.0,
    low24h = 66_100.0,
    marketCap = 1_320_000_000_000.0,
    volume24h = 28_500_000_000.0,
    logoUrl = null,
    isFavorite = false,
    lastUpdated = System.currentTimeMillis(),
)

@Preview(showBackground = true)
@Composable
private fun DetailPreview() {
    TradingAppTheme {
        MarketDetailContent(
            state = MarketDetailState(asset = fakeAsset(), isLoading = false, isOffline = false),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailLoadingPreview() {
    TradingAppTheme {
        MarketDetailContent(state = MarketDetailState(), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailErrorPreview() {
    TradingAppTheme {
        MarketDetailContent(
            state = MarketDetailState(isLoading = false, error = "Asset not found"),
            onEvent = {},
        )
    }
}
