package com.tradingapp.trading

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.designsystem.PriceDown
import com.tradingapp.designsystem.PriceUp
import com.tradingapp.designsystem.Spacing
import com.tradingapp.domain.model.Order
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.OrderStatus
import com.tradingapp.domain.model.Portfolio
import com.tradingapp.domain.model.Position
import com.tradingapp.ui.components.EmptyState
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.components.OfflineBanner
import com.tradingapp.ui.components.PercentageBadge
import com.tradingapp.ui.components.SectionHeader
import com.tradingapp.ui.theme.TradingAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTrade: (String) -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PortfolioEffect.NavigateBack -> onNavigateBack()
                is PortfolioEffect.NavigateToTrade -> onNavigateToTrade(effect.symbol)
            }
        }
    }

    PortfolioContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioContent(state: PortfolioState, onEvent: (PortfolioEvent) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Portfolio", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(PortfolioEvent.NavigateBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                OfflineBanner(isOffline = state.isOffline, lastUpdatedMs = null)
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.portfolio == null -> LoadingIndicator(Modifier.padding(padding))
            state.error != null && state.portfolio == null -> ErrorState(
                message = state.error,
                onRetry = { onEvent(PortfolioEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            else -> PortfolioBody(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun PortfolioBody(state: PortfolioState, onEvent: (PortfolioEvent) -> Unit, modifier: Modifier = Modifier) {
    val portfolio = state.portfolio
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (portfolio != null) {
            item { PortfolioSummaryCard(portfolio = portfolio, modifier = Modifier.padding(Spacing.md)) }

            item {
                SectionHeader(
                    title = "Cash Balance",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            item {
                CashBalanceCard(
                    balance = portfolio.cashBalance,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
            }

            item {
                SectionHeader(
                    title = "Positions (${portfolio.positions.size})",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            if (portfolio.positions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No open positions",
                        subtitle = "Buy an asset to open a position.",
                        modifier = Modifier.padding(Spacing.lg),
                    )
                }
            } else {
                items(portfolio.positions, key = { it.symbol }) { position ->
                    PositionRow(
                        position = position,
                        onTradeClick = { onEvent(PortfolioEvent.TradeAsset(position.symbol)) },
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
                }
            }
        }

        item {
            SectionHeader(
                title = "Order History (${state.orders.size})",
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )
        }
        if (state.orders.isEmpty()) {
            item {
                EmptyState(
                    title = "No orders yet",
                    subtitle = "Place your first order to see history here.",
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        } else {
            items(state.orders, key = { it.id }) { order ->
                OrderHistoryRow(
                    order = order,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun PortfolioSummaryCard(portfolio: Portfolio, modifier: Modifier = Modifier) {
    val pnlColor = if (portfolio.totalUnrealizedPnL >= 0.0) PriceUp else PriceDown
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "Total Portfolio Value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                formatUsd(portfolio.totalValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                val pnlPrefix = if (portfolio.totalUnrealizedPnL >= 0) "+" else ""
                Text(
                    text = "$pnlPrefix${formatUsd(portfolio.totalUnrealizedPnL)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = pnlColor,
                    fontWeight = FontWeight.Medium,
                )
                PercentageBadge(
                    changePercent = portfolio.totalUnrealizedPnLPct,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CashBalanceCard(balance: Double, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
        ) {
            Text("Available cash", style = MaterialTheme.typography.bodyMedium)
            Text(formatUsd(balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PositionRow(position: Position, onTradeClick: () -> Unit, modifier: Modifier = Modifier) {
    val pnlColor: Color = if (position.unrealizedPnL >= 0.0) PriceUp else PriceDown
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(position.symbol, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${TradingViewModel.formatQuantity(position.quantity)} ${position.symbol}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Text(
                formatUsd(position.totalValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                val pnlPrefix = if (position.unrealizedPnL >= 0) "+" else ""
                Text(
                    text = "$pnlPrefix${formatUsd(position.unrealizedPnL)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = pnlColor,
                )
                PercentageBadge(
                    changePercent = position.unrealizedPnLPct,
                    textStyle = MaterialTheme.typography.labelSmall,
                )
            }
        }
        TextButton(onClick = onTradeClick) { Text("Trade", style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun OrderHistoryRow(order: Order, modifier: Modifier = Modifier) {
    val sideColor = if (order.side == OrderSide.BUY) PriceUp else PriceDown
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    order.side.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = sideColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(order.symbol, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                formatTimestamp(order.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatUsd(order.totalValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "${TradingViewModel.formatQuantity(order.quantity)} @ ${formatUsd(order.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Helpers ---

private fun formatUsd(amount: Double): String = "${"$"}${"%.2f".format(amount)}"

private fun formatTimestamp(ms: Long): String = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))

// --- Previews ---

@Suppress("MagicNumber")
private fun fakePortfolio() = Portfolio(
    cashBalance = 2_450.23,
    positions = listOf(
        Position("BTC", 0.1483, 65_000.0, 67_234.5, 9_971.8, 330.8, 0.51),
        Position("ETH", 1.2, 3_000.0, 3_200.0, 3_840.0, 240.0, 6.67),
    ),
    totalValue = 16_262.03,
    totalUnrealizedPnL = 570.8,
    totalUnrealizedPnLPct = 3.64,
)

private fun fakeOrder(side: OrderSide, symbol: String, qty: Double, price: Double) = Order(
    id = "1",
    symbol = symbol,
    side = side,
    quantity = qty,
    price = price,
    totalValue = qty * price,
    status = OrderStatus.FILLED,
    timestamp = System.currentTimeMillis() - 3_600_000,
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortfolioPreview() {
    TradingAppTheme {
        PortfolioContent(
            state = PortfolioState(
                portfolio = fakePortfolio(),
                orders = listOf(
                    fakeOrder(OrderSide.BUY, "BTC", 0.1483, 65_000.0),
                    fakeOrder(OrderSide.BUY, "ETH", 1.2, 3_000.0),
                ),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Empty portfolio", showBackground = true)
@Composable
private fun PortfolioEmptyPreview() {
    TradingAppTheme {
        PortfolioContent(
            state = PortfolioState(
                portfolio = Portfolio(10_000.0, emptyList(), 10_000.0, 0.0, 0.0),
                orders = emptyList(),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
