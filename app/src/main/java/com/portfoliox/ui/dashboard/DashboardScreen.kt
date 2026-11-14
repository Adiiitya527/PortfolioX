package com.portfoliox.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.core.Formatters
import com.portfoliox.domain.PortfolioCalculator
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.PortfolioSummary
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.PriceChangeText
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.theme.PortfolioXTheme
import java.math.BigDecimal

/**
 * Stateful entry point: obtains the ViewModel and collects its state.
 * collectAsStateWithLifecycle stops collecting (and so stops price updates) while the app is
 * in the background.
 */
@Composable
fun DashboardScreen(
    onStockClick: (symbol: String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState, onStockClick, onSearchClick, onRetry = viewModel::retry)
}

/** Stateless content: depends only on its parameters, so it can be previewed with any state. */
@Composable
fun DashboardContent(
    uiState: UiState<DashboardData>,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            PortfolioXTopBar(
                title = stringResource(R.string.nav_portfolio),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(uiState.messageRes, contentModifier, onRetry = onRetry)
            // Not produced: the dashboard always has a summary, even with no holdings.
            UiState.Empty -> ErrorView(R.string.error_generic, contentModifier, onRetry = onRetry)
            is UiState.Success -> DashboardList(uiState.data, onStockClick, onSearchClick, contentModifier)
        }
    }
}

@Composable
private fun DashboardList(
    data: DashboardData,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One LazyColumn holds the summary, a header and the rows, so the whole page scrolls together.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { SummaryCard(data.summary) }
        item {
            Text(
                text = stringResource(R.string.dashboard_holdings_title, data.holdings.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        if (data.holdings.isEmpty()) {
            item { NoHoldings(onSearchClick) }
        } else {
            items(data.holdings, key = { it.symbol }) { holding ->
                HoldingRow(holding, onClick = { onStockClick(holding.symbol) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: PortfolioSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dashboard_total_value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = Formatters.currency(summary.totalValue),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceChangeText(summary.dayChange, summary.dayChangePercent, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = " " + stringResource(R.string.detail_today),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryFigure(stringResource(R.string.dashboard_invested), Formatters.currency(summary.invested), Modifier.weight(1f))
                SummaryFigure(stringResource(R.string.dashboard_holdings_value), Formatters.currency(summary.holdingsValue), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    FigureLabel(stringResource(R.string.dashboard_unrealized_pnl))
                    PriceChangeText(summary.unrealizedPnl, summary.unrealizedPnlPercent, style = MaterialTheme.typography.titleMedium)
                }
                SummaryFigure(stringResource(R.string.portfolio_cash_available), Formatters.currency(summary.cash), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FigureLabel(label)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun FigureLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
    )
}

/** Left: symbol and "10 shares · avg $190.37". Right: market value and P&L. */
@Composable
private fun HoldingRow(holding: DashboardHolding, onClick: () -> Unit) {
    val valuation = holding.valuation
    val shares = valuation.holding.quantity
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(holding.symbol, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_allocation, holding.allocationPercent.toPlainString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = holding.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_shares_at_average,
                    shares,
                    shares,
                    Formatters.currency(valuation.holding.averageCost),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatters.currency(valuation.marketValue), style = MaterialTheme.typography.titleMedium)
            PriceChangeText(valuation.unrealizedPnl, valuation.unrealizedPnlPercent, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NoHoldings(onSearchClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_no_holdings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onSearchClick) { Text(stringResource(R.string.action_search)) }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun DashboardPreview() {
    val valuations = listOf(
        HoldingValuation(Holding("IBKR", 10, BigDecimal("170.00")), BigDecimal("188.40"), BigDecimal("187.55")),
        HoldingValuation(Holding("TSLA", 4, BigDecimal("265.00")), BigDecimal("251.75"), BigDecimal("255.10")),
    )
    val summary = PortfolioCalculator.summarize(valuations, BigDecimal("97240.00"))
    PortfolioXTheme {
        DashboardContent(
            uiState = UiState.Success(
                DashboardData(
                    summary = summary,
                    holdings = valuations.map {
                        DashboardHolding(it, it.holding.symbol + " Inc.", BigDecimal("50.00"))
                    },
                ),
            ),
            onStockClick = {},
            onSearchClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardEmptyPreview() {
    PortfolioXTheme {
        DashboardContent(
            uiState = UiState.Success(DashboardData(PortfolioCalculator.summarize(emptyList(), BigDecimal("100000")), emptyList())),
            onStockClick = {},
            onSearchClick = {},
            onRetry = {},
        )
    }
}
