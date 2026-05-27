package com.tradingapp.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.domain.model.Asset
import com.tradingapp.ui.components.EmptyState
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.theme.PriceDown
import com.tradingapp.ui.theme.PriceUp
import com.tradingapp.ui.theme.TradingAppTheme

@Composable
fun SearchScreen(
    onAssetClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetail -> onAssetClick(effect.symbol)
                is SearchEffect.NavigateBack -> onNavigateBack()
                is SearchEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    SearchContent(
        state = state,
        snackbarHost = snackbarHost,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    state: SearchState,
    snackbarHost: SnackbarHostState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                placeholder = { Text("Symbol or name…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onEvent(SearchEvent.ClearQuery) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                // Horizontally scrollable filter chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AssetFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.activeFilter == filter,
                            onClick = { onEvent(SearchEvent.FilterSelected(filter)) },
                            label = { Text(filter.label) },
                        )
                    }
                }

                SortMenu(
                    activeSort = state.sortOrder,
                    onSort = { onEvent(SearchEvent.SortSelected(it)) },
                )
            }

            when {
                state.isLoading && state.results.isEmpty() ->
                    LoadingIndicator()

                state.error != null && state.results.isEmpty() ->
                    ErrorState(message = state.error)

                state.results.isEmpty() ->
                    EmptyState(
                        title = "No assets found",
                        subtitle = "Try different terms or filters.",
                    )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.symbol }) { asset ->
                        SearchAssetRow(
                            asset = asset,
                            onClick = { onEvent(SearchEvent.AssetClicked(asset.symbol)) },
                            onFavClick = {
                                onEvent(SearchEvent.ToggleFavorite(asset.symbol, !asset.isFavorite))
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(activeSort: SortOrder, onSort: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Sort")
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortOrder.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.label,
                            color = if (activeSort == sort) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        onSort(sort)
                        expanded = false
                    },
                )
            }
        }
    }
}

// Same visual style as WatchlistScreen.AssetRow — kept local (single caller, no premature extraction).
@Composable
private fun SearchAssetRow(asset: Asset, onClick: () -> Unit, onFavClick: () -> Unit) {
    val priceColor = if (asset.isUp) PriceUp else PriceDown
    val trendIcon = if (asset.isUp) {
        Icons.AutoMirrored.Filled.TrendingUp
    } else {
        Icons.AutoMirrored.Filled.TrendingDown
    }
    val pctSign = if (asset.isUp) "+" else ""
    val pctLabel = "$pctSign${"%.2f".format(asset.priceChangePct24h)}%"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = asset.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "%.2f".format(asset.currentPrice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = priceColor,
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(
                    text = pctLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = priceColor,
                )
            }
        }

        IconButton(onClick = onFavClick) {
            Icon(
                imageVector = if (asset.isFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (asset.isFavorite) {
                    "Remove from favorites"
                } else {
                    "Add to favorites"
                },
                tint = if (asset.isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// Readable chip and menu labels for enum values.
private val AssetFilter.label: String get() = when (this) {
    AssetFilter.ALL -> "All"
    AssetFilter.FAVORITES -> "Favorites"
    AssetFilter.GAINERS -> "Gainers"
    AssetFilter.LOSERS -> "Losers"
}

private val SortOrder.label: String get() = when (this) {
    SortOrder.NONE -> "Default"
    SortOrder.PRICE_CHANGE_DESC -> "Price Change ↓"
    SortOrder.PRICE_CHANGE_ASC -> "Price Change ↑"
    SortOrder.VOLUME_DESC -> "Volume ↓"
    SortOrder.VOLUME_ASC -> "Volume ↑"
}

// --- Previews ---

private fun fakeAsset(symbol: String, name: String, price: Double, pct: Double, fav: Boolean = false) = Asset(
    symbol = symbol,
    name = name,
    currentPrice = price,
    priceChange24h = price * pct / 100,
    priceChangePct24h = pct,
    marketCap = 1_000_000_000.0,
    volume24h = 50_000_000.0,
    logoUrl = null,
    isFavorite = fav,
    lastUpdated = System.currentTimeMillis(),
)

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    TradingAppTheme {
        SearchContent(
            state = SearchState(
                isLoading = false,
                results = listOf(
                    fakeAsset("BTC", "Bitcoin", 67_500.0, 2.34, fav = true),
                    fakeAsset("ETH", "Ethereum", 3_450.0, -1.12),
                    fakeAsset("SOL", "Solana", 175.0, 5.67),
                ),
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchLoadingPreview() {
    TradingAppTheme {
        SearchContent(
            state = SearchState(),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchEmptyPreview() {
    TradingAppTheme {
        SearchContent(
            state = SearchState(isLoading = false, query = "xyz"),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
        )
    }
}
