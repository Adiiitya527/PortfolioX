package com.portfoliox.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.core.Formatters
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.changeColor
import com.portfoliox.ui.theme.PortfolioXTheme
import com.portfoliox.ui.theme.financialColors
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HistoryScreen(
    onStockClick: (symbol: String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onStockClick = onStockClick,
        onSearchClick = onSearchClick,
        onRetry = viewModel::retry,
    )
}

@Composable
fun HistoryContent(
    uiState: UiState<HistoryData>,
    onFilterSelected: (HistoryFilter) -> Unit,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(topBar = { PortfolioXTopBar(title = stringResource(R.string.nav_history)) }) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(uiState.messageRes, contentModifier, onRetry = onRetry)
            UiState.Empty -> EmptyView(
                title = stringResource(R.string.history_empty_title),
                message = stringResource(R.string.history_empty_message),
                actionLabel = stringResource(R.string.action_search),
                onAction = onSearchClick,
                modifier = contentModifier,
            )
            is UiState.Success -> HistoryList(uiState.data, onFilterSelected, onStockClick, contentModifier)
        }
    }
}

@Composable
private fun HistoryList(
    data: HistoryData,
    onFilterSelected: (HistoryFilter) -> Unit,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { RealizedSummary(data.totalRealizedPnl, data.tradeCount) }
        item { FilterRow(data.filter, onFilterSelected) }

        if (data.days.isEmpty()) {
            item { NoMatches(data.filter) }
        }

        data.days.forEach { day ->
            // stickyHeader: the date stays pinned at the top while that day's trades scroll under it.
            stickyHeader(key = "header-${day.date}") { DayHeader(day.date, data.today) }
            items(day.transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { onStockClick(transaction.symbol) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RealizedSummary(totalRealizedPnl: BigDecimal, tradeCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.history_realized_pnl),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Formatters.signedCurrency(totalRealizedPnl),
                    style = MaterialTheme.typography.headlineSmall,
                    color = changeColor(totalRealizedPnl),
                )
            }
            Text(
                text = pluralStringResource(R.plurals.history_trade_count, tradeCount, tradeCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Filter chips: like toggle buttons in a group, where exactly one is selected. */
@Composable
private fun FilterRow(selected: HistoryFilter, onFilterSelected: (HistoryFilter) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelRes())) },
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, today: LocalDate) {
    val label = when (val relative = relativeDay(date, today)) {
        RelativeDay.Today -> stringResource(R.string.history_today)
        RelativeDay.Yesterday -> stringResource(R.string.history_yesterday)
        is RelativeDay.On -> Formatters.date(relative.date)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            // Opaque background so rows scrolling underneath don't show through the pinned header.
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** BUY/SELL badge, symbol and "10 × $190.37 · 14:32" on the left; total and realized P&L on the right. */
@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(transaction.type)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.history_quantity_price_time,
                    transaction.quantity,
                    Formatters.currency(transaction.price),
                    Formatters.time(transaction.timestamp),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatters.currency(transaction.total), style = MaterialTheme.typography.titleMedium)
            transaction.realizedPnl?.let { pnl ->
                Text(
                    text = stringResource(R.string.history_realized_short, Formatters.signedCurrency(pnl)),
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor(pnl),
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: TradeType) {
    val (label, color) = when (type) {
        TradeType.BUY -> stringResource(R.string.trade_buy) to MaterialTheme.financialColors.gain
        TradeType.SELL -> stringResource(R.string.trade_sell) to MaterialTheme.financialColors.loss
    }
    Box(
        modifier = Modifier
            .width(48.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun NoMatches(filter: HistoryFilter) {
    Text(
        text = stringResource(if (filter == HistoryFilter.SELLS) R.string.history_no_sells else R.string.history_no_buys),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    )
}

private fun HistoryFilter.labelRes(): Int = when (this) {
    HistoryFilter.ALL -> R.string.history_filter_all
    HistoryFilter.BUYS -> R.string.history_filter_buys
    HistoryFilter.SELLS -> R.string.history_filter_sells
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun HistoryPreview() {
    val today = LocalDate.of(2026, 11, 14)
    val at = { day: Int, hour: Int -> today.minusDays(day.toLong()).atTime(hour, 15).toInstant(ZoneOffset.UTC) }
    val transactions = listOf(
        Transaction(3, "IBKR", TradeType.SELL, 4, BigDecimal("190.21"), BigDecimal("760.84"), BigDecimal("-0.64"), at(0, 14)),
        Transaction(2, "IBKR", TradeType.BUY, 10, BigDecimal("190.37"), BigDecimal("1903.70"), null, at(0, 10)),
        Transaction(1, "KO", TradeType.BUY, 100, BigDecimal("68.55"), BigDecimal("6855.00"), null, at(1, 9)),
    )
    PortfolioXTheme {
        HistoryContent(
            uiState = UiState.Success(
                HistoryData(HistoryFilter.ALL, groupByDay(transactions, ZoneOffset.UTC), BigDecimal("-0.64"), 3, today),
            ),
            onFilterSelected = {},
            onStockClick = {},
            onSearchClick = {},
            onRetry = {},
        )
    }
}
