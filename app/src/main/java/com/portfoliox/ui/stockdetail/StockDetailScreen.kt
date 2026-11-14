package com.portfoliox.ui.stockdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.core.Formatters
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.PriceChangeText
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.changeColor
import com.portfoliox.ui.theme.PortfolioXTheme
import java.math.BigDecimal
import java.time.Instant

@Composable
fun StockDetailScreen(
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = viewModel(factory = StockDetailViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chartState by viewModel.chartState.collectAsStateWithLifecycle()

    StockDetailContent(
        symbol = viewModel.symbol,
        uiState = uiState,
        chartState = chartState,
        onBack = onBack,
        onToggleWatchlist = viewModel::toggleWatchlist,
        onSelectRange = viewModel::selectRange,
        onRetry = viewModel::retry,
        onRetryChart = viewModel::retryChart,
    )
}

@Composable
fun StockDetailContent(
    symbol: String,
    uiState: UiState<StockDetail>,
    chartState: ChartUiState,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onSelectRange: (ChartRange) -> Unit,
    onRetry: () -> Unit,
    onRetryChart: () -> Unit,
) {
    Scaffold(
        topBar = {
            PortfolioXTopBar(
                title = symbol,
                onBack = onBack,
                actions = {
                    if (uiState is UiState.Success) {
                        WatchlistToggle(isWatched = uiState.data.isWatched, onClick = onToggleWatchlist)
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(uiState.messageRes, contentModifier, onRetry = onRetry)
            // Never produced for this screen, but `when` must be exhaustive; fail safe, not with a spinner forever.
            UiState.Empty -> ErrorView(R.string.error_generic, contentModifier, onRetry = onRetry)
            is UiState.Success -> DetailBody(uiState.data, chartState, onSelectRange, onRetryChart, contentModifier)
        }
    }
}

@Composable
private fun DetailBody(
    detail: StockDetail,
    chartState: ChartUiState,
    onSelectRange: (ChartRange) -> Unit,
    onRetryChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // verticalScroll: a plain scrollable column. A LazyColumn isn't needed for a fixed, short list of sections.
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        QuoteHeader(detail.stock, detail.quote)
        ChartSection(detail.stock.symbol, chartState, onSelectRange, onRetryChart)
        detail.position?.let { PositionCard(it) }
        KeyStatsCard(detail.stock, detail.quote)
    }
}

@Composable
private fun QuoteHeader(stock: Stock, quote: Quote?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stock.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = quote?.price?.let(Formatters::currency) ?: "—",
            style = MaterialTheme.typography.displaySmall,
        )
        if (quote != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceChangeText(quote.change, quote.changePercent, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = " " + stringResource(R.string.detail_today),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The selected point is UI-only state: it changes on every drag frame and nothing else needs
 * it, so it lives here in `remember` rather than in the ViewModel. `remember(points)` resets
 * the selection when new data arrives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartSection(
    symbol: String,
    chartState: ChartUiState,
    onSelectRange: (ChartRange) -> Unit,
    onRetry: () -> Unit,
) {
    val points = (chartState.points as? UiState.Success)?.data.orEmpty()
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val rangeChange = remember(points) { RangeChange.of(points) }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        ChartCaption(
            selectedPoint = selectedIndex?.let(points::getOrNull),
            rangeChange = rangeChange,
            range = chartState.range,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val chartPoints = chartState.points) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> ChartMessage(chartPoints.messageRes, onRetry)
                UiState.Empty -> ChartMessage(R.string.chart_no_data, onRetry = null)
                is UiState.Success -> PriceChart(
                    points = chartPoints.data,
                    lineColor = rangeChange?.let { changeColor(it.change) } ?: MaterialTheme.colorScheme.primary,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { selectedIndex = it },
                    contentDescription = stringResource(R.string.chart_description, symbol, stringResource(chartState.range.labelRes())),
                )
            }
        }

        // Segmented buttons: a single-choice control, like a radio group styled as joined buttons.
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ChartRange.entries.forEachIndexed { index, range ->
                SegmentedButton(
                    selected = range == chartState.range,
                    onClick = { onSelectRange(range) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartRange.entries.size),
                    icon = {}, // no checkmark; the filled background already shows the selection
                ) {
                    Text(range.label)
                }
            }
        }
    }
}

/** Above the chart: the touched point's price and date, or the change over the whole range. */
@Composable
private fun ChartCaption(selectedPoint: PricePoint?, rangeChange: RangeChange?, range: ChartRange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            selectedPoint != null -> {
                // Daily points have no meaningful time of day, so show the date only.
                val whenText = if (range.step.toHours() >= 24) {
                    Formatters.date(selectedPoint.timestamp)
                } else {
                    Formatters.dateTime(selectedPoint.timestamp)
                }
                Text(
                    text = "${Formatters.currency(selectedPoint.close)} · $whenText",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            rangeChange != null -> {
                PriceChangeText(rangeChange.change, rangeChange.changePercent, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = " " + stringResource(range.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartMessage(@StringRes messageRes: Int, onRetry: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

@Composable
private fun PositionCard(position: HoldingValuation) {
    SectionCard(title = stringResource(R.string.section_position)) {
        StatRow(stringResource(R.string.position_shares), position.holding.quantity.toString())
        StatRow(stringResource(R.string.position_average_cost), Formatters.currency(position.holding.averageCost))
        StatRow(stringResource(R.string.position_market_value), Formatters.currency(position.marketValue))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.position_unrealized_pnl), color = MaterialTheme.colorScheme.onSurfaceVariant)
            PriceChangeText(position.unrealizedPnl, position.unrealizedPnlPercent)
        }
    }
}

@Composable
private fun KeyStatsCard(stock: Stock, quote: Quote?) {
    SectionCard(title = stringResource(R.string.section_key_stats)) {
        fun money(value: BigDecimal?) = value?.let(Formatters::currency) ?: "—"
        StatRow(stringResource(R.string.stat_open), money(quote?.open))
        StatRow(stringResource(R.string.stat_day_high), money(quote?.dayHigh))
        StatRow(stringResource(R.string.stat_day_low), money(quote?.dayLow))
        StatRow(stringResource(R.string.stat_previous_close), money(quote?.previousClose))
        StatRow(stringResource(R.string.stat_exchange), stock.exchange)
        StatRow(stringResource(R.string.stat_sector), stock.sector)
    }
}

/** A titled card. `content: @Composable () -> Unit` is a slot for the rows inside. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun WatchlistToggle(isWatched: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (isWatched) {
            Icon(Icons.Filled.Star, stringResource(R.string.watchlist_remove), tint = MaterialTheme.colorScheme.primary)
        } else {
            Icon(painterResource(R.drawable.ic_star_border), stringResource(R.string.watchlist_add))
        }
    }
}

/** Display text for a range, e.g. "past month". */
@StringRes
private fun ChartRange.labelRes(): Int = when (this) {
    ChartRange.WEEK -> R.string.range_past_week
    ChartRange.MONTH -> R.string.range_past_month
    ChartRange.QUARTER -> R.string.range_past_quarter
    ChartRange.YEAR -> R.string.range_past_year
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun StockDetailPreview() {
    val stock = Stock("IBKR", "Interactive Brokers Group, Inc.", "NASDAQ", "Financials")
    val quote = Quote("IBKR", BigDecimal("188.40"), BigDecimal("187.55"), BigDecimal("187.90"), BigDecimal("189.10"), BigDecimal("186.80"), Instant.EPOCH)
    val holding = Holding("IBKR", 10, BigDecimal("170.00"))
    val points = listOf(170, 174, 172, 178, 181, 179, 185, 188).mapIndexed { i, c ->
        PricePoint(Instant.ofEpochSecond(i * 86_400L), BigDecimal(c))
    }
    PortfolioXTheme {
        StockDetailContent(
            symbol = "IBKR",
            uiState = UiState.Success(StockDetail(stock, quote, isWatched = true, position = HoldingValuation.from(holding, quote))),
            chartState = ChartUiState(ChartRange.MONTH, UiState.Success(points)),
            onBack = {},
            onToggleWatchlist = {},
            onSelectRange = {},
            onRetry = {},
            onRetryChart = {},
        )
    }
}
