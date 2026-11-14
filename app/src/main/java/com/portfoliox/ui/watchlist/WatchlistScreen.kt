package com.portfoliox.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.StockRow
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.theme.PortfolioXTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

@Composable
fun WatchlistScreen(
    onSearchClick: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    viewModel: WatchlistViewModel = viewModel(factory = WatchlistViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WatchlistContent(
        uiState = uiState,
        onSearchClick = onSearchClick,
        onStockClick = onStockClick,
        onRemove = viewModel::remove,
        onUndoRemove = viewModel::undoRemove,
        onRetry = viewModel::retry,
    )
}

@Composable
fun WatchlistContent(
    uiState: UiState<List<WatchlistItem>>,
    onSearchClick: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    onRemove: (symbol: String) -> Unit,
    onUndoRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    // remember keeps the same object across recompositions (like useRef / useMemo in React).
    val snackbarHostState = remember { SnackbarHostState() }
    // A coroutine scope tied to this composable, for launching UI work such as showing a snackbar.
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.watchlist_removed)
    val undoLabel = stringResource(R.string.action_undo)

    // The snackbar is UI behaviour, so the screen drives it: remove, show "Undo", undo if tapped.
    val removeWithUndo: (String) -> Unit = { symbol ->
        onRemove(symbol)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss() // only one undo at a time
            val result = snackbarHostState.showSnackbar(
                message = removedMessage.format(symbol),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndoRemove()
        }
    }

    Scaffold(
        topBar = {
            PortfolioXTopBar(
                title = stringResource(R.string.nav_watchlist),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(uiState.messageRes, contentModifier, onRetry = onRetry)
            UiState.Empty -> EmptyView(
                title = stringResource(R.string.watchlist_empty_title),
                message = stringResource(R.string.watchlist_empty_message),
                actionLabel = stringResource(R.string.action_search),
                onAction = onSearchClick,
                modifier = contentModifier,
            )
            is UiState.Success -> WatchlistList(
                items = uiState.data,
                onStockClick = onStockClick,
                onRemove = removeWithUndo,
                modifier = contentModifier,
            )
        }
    }
}

/**
 * LazyColumn only composes the rows currently on screen, like a virtualised list
 * (react-window). `key` gives each row a stable identity, so when a row is removed Compose
 * keeps the right state attached to the remaining rows.
 */
@Composable
private fun WatchlistList(
    items: List<WatchlistItem>,
    onStockClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items, key = { it.stock.symbol }) { item ->
            SwipeToRemoveRow(item = item, onClick = { onStockClick(item.stock.symbol) }, onRemove = { onRemove(item.stock.symbol) })
            HorizontalDivider()
        }
    }
}

/** Swipe right-to-left to remove. The red background with a bin icon is revealed underneath. */
@Composable
private fun SwipeToRemoveRow(
    item: WatchlistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onRemove() },
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        StockRow(
            symbol = item.stock.symbol,
            name = item.stock.name,
            price = item.quote?.price,
            change = item.quote?.change,
            changePercent = item.quote?.changePercent,
            onClick = onClick,
            // Opaque background so the red layer only shows where the row has moved away.
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistPreview() {
    val stock = Stock("AAPL", "Apple Inc.", "NASDAQ", "Technology")
    val quote = Quote(
        symbol = "AAPL",
        price = BigDecimal("233.10"),
        previousClose = BigDecimal("231.45"),
        open = BigDecimal("231.80"),
        dayHigh = BigDecimal("234.00"),
        dayLow = BigDecimal("230.90"),
        timestamp = Instant.EPOCH,
    )
    PortfolioXTheme {
        WatchlistContent(
            uiState = UiState.Success(
                listOf(
                    WatchlistItem(stock, quote),
                    WatchlistItem(Stock("IBKR", "Interactive Brokers Group, Inc.", "NASDAQ", "Financials"), null),
                ),
            ),
            onSearchClick = {},
            onStockClick = {},
            onRemove = {},
            onUndoRemove = {},
            onRetry = {},
        )
    }
}
