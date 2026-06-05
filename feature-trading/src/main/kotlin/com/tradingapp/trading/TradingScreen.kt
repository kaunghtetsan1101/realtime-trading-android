package com.tradingapp.trading

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradingapp.designsystem.PriceDown
import com.tradingapp.designsystem.PriceUp
import com.tradingapp.designsystem.Spacing
import com.tradingapp.domain.model.OrderSide
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.model.ValidationError
import com.tradingapp.ui.components.ErrorState
import com.tradingapp.ui.components.LoadingIndicator
import com.tradingapp.ui.components.OfflineBanner
import com.tradingapp.ui.components.PrimaryActionButton
import com.tradingapp.ui.theme.TradingAppTheme

@Composable
fun TradingScreen(onNavigateBack: () -> Unit, viewModel: TradingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TradingEffect.NavigateBack -> onNavigateBack()
                is TradingEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    TradingContent(
        state = state,
        snackbarHost = snackbarHost,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TradingContent(state: TradingState, snackbarHost: SnackbarHostState, onEvent: (TradingEvent) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val title = if (state.assetName.isNotBlank()) "${state.symbol} · Trade" else "Trade"

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(TradingEvent.NavigateBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                OfflineBanner(isOffline = state.isOffline, lastUpdatedMs = state.lastSyncedAt)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        when {
            state.isLoading && state.currentPrice == 0.0 -> LoadingIndicator(Modifier.padding(padding))
            state.error != null && state.currentPrice == 0.0 -> ErrorState(
                message = state.error,
                onRetry = { onEvent(TradingEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            else -> TradingBody(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (state.isReviewVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onEvent(TradingEvent.DismissReview) },
            sheetState = sheetState,
        ) {
            OrderConfirmationSheet(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun TradingBody(state: TradingState, onEvent: (TradingEvent) -> Unit, modifier: Modifier = Modifier) {
    val quantity = state.quantityInput.toDoubleOrNull() ?: 0.0
    val orderTotal = quantity * state.currentPrice
    val isQuantityValid = state.quantityInput.isNotBlank() && state.validationError == null && quantity > 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PriceHeader(price = state.currentPrice)

        TradeSideSelector(selected = state.selectedSide, onSelect = { onEvent(TradingEvent.SideSelected(it)) })

        QuantityInput(
            value = state.quantityInput,
            symbol = state.symbol,
            error = state.validationError,
            onValueChange = { onEvent(TradingEvent.QuantityChanged(it)) },
        )

        AvailableRow(state = state)

        QuickFillRow(onFill = { fraction -> onEvent(TradingEvent.QuickFillSelected(fraction)) })

        if (isQuantityValid) {
            OrderSummaryCard(
                side = state.selectedSide,
                quantity = quantity,
                price = state.currentPrice,
                total = orderTotal,
                symbol = state.symbol,
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        PrimaryActionButton(
            text = "Review Order",
            enabled = isQuantityValid && !state.isPlacingOrder,
            onClick = { onEvent(TradingEvent.ReviewOrder) },
        )
    }
}

@Composable
private fun PriceHeader(price: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Current Price",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatUsd(price),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TradeSideSelector(selected: OrderSide, onSelect: (OrderSide) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buySelected = selected == OrderSide.BUY
        Button(
            onClick = { onSelect(OrderSide.BUY) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (buySelected) PriceUp else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (buySelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            modifier = Modifier.weight(1f),
        ) { Text("BUY", fontWeight = FontWeight.Bold) }

        Button(
            onClick = { onSelect(OrderSide.SELL) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!buySelected) PriceDown else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (!buySelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            modifier = Modifier.weight(1f),
        ) { Text("SELL", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun QuantityInput(value: String, symbol: String, error: ValidationError?, onValueChange: (String) -> Unit) {
    val errorMessage = error?.toMessage()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Quantity ($symbol)") },
        placeholder = { Text("0.00000000") },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AvailableRow(state: TradingState) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (state.selectedSide == OrderSide.BUY) "Available cash" else "Available position",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (state.selectedSide == OrderSide.BUY) {
                formatUsd(state.cashBalance)
            } else {
                val held = state.existingPosition?.quantity ?: 0.0
                "${TradingViewModel.formatQuantity(held)} ${state.symbol}"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun QuickFillRow(onFill: (Double) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "MAX").forEach { (fraction, label) ->
            OutlinedButton(
                onClick = { onFill(fraction) },
                modifier = Modifier.weight(1f),
            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun OrderSummaryCard(side: OrderSide, quantity: Double, price: Double, total: Double, symbol: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "Order Summary",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xxs))
            SummaryRow("Side", if (side == OrderSide.BUY) "BUY" else "SELL")
            SummaryRow("Quantity", "${TradingViewModel.formatQuantity(quantity)} $symbol")
            SummaryRow("Price", formatUsd(price))
            SummaryRow("Total", formatUsd(total))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderConfirmationSheet(state: TradingState, onEvent: (TradingEvent) -> Unit) {
    val quantity = state.quantityInput.toDoubleOrNull() ?: 0.0
    val total = quantity * state.currentPrice

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Confirm Order", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

        val sideColor = if (state.selectedSide == OrderSide.BUY) PriceUp else PriceDown
        Text(
            text = "${state.selectedSide.name} ${state.symbol}",
            style = MaterialTheme.typography.titleMedium,
            color = sideColor,
            fontWeight = FontWeight.SemiBold,
        )

        SummaryRow("Quantity", "${TradingViewModel.formatQuantity(quantity)} ${state.symbol}")
        SummaryRow("Execution price", "${formatUsd(state.currentPrice)} (market)")
        SummaryRow("Total", formatUsd(total))

        Spacer(Modifier.height(Spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onEvent(TradingEvent.DismissReview) },
                modifier = Modifier.weight(1f),
            ) { Text("Cancel") }

            Button(
                onClick = { onEvent(TradingEvent.ConfirmOrder) },
                enabled = !state.isPlacingOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.selectedSide == OrderSide.BUY) PriceUp else PriceDown,
                ),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isPlacingOrder) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm")
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

// --- Helpers ---

private fun ValidationError.toMessage(): String = when (this) {
    ValidationError.EMPTY_QUANTITY -> ""
    ValidationError.INVALID_QUANTITY -> "Enter a valid number"
    ValidationError.ZERO_QUANTITY -> "Quantity must be greater than 0"
    ValidationError.INSUFFICIENT_BALANCE -> "Insufficient cash balance"
    ValidationError.INSUFFICIENT_POSITION -> "Insufficient position"
}

private fun formatUsd(amount: Double): String = "${"$"}${"%.2f".format(amount)}"

// --- Previews ---

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TradingPreview() {
    TradingAppTheme {
        TradingContent(
            state = TradingState(
                symbol = "BTC",
                assetName = "Bitcoin",
                currentPrice = 67_234.50,
                cashBalance = 10_000.0,
                existingPosition = null,
                selectedSide = OrderSide.BUY,
                quantityInput = "0.1",
                isLoading = false,
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@Preview(name = "Light — sell with position", showBackground = true)
@Composable
private fun TradingSellPreview() {
    TradingAppTheme {
        TradingContent(
            state = TradingState(
                symbol = "ETH",
                assetName = "Ethereum",
                currentPrice = 3_200.0,
                cashBalance = 500.0,
                existingPosition = Position("ETH", 1.5, 2_800.0),
                selectedSide = OrderSide.SELL,
                quantityInput = "0.5",
                isLoading = false,
            ),
            snackbarHost = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}
