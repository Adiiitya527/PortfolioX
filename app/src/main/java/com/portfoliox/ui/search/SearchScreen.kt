package com.portfoliox.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.domain.model.Stock
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.StockRow
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.theme.PortfolioXTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // rememberSaveable survives rotation and process death (it's stored in the saved instance state).
    var query by rememberSaveable { mutableStateOf("") }

    SearchContent(
        query = query,
        onQueryChange = { text ->
            query = text
            viewModel.onQueryChange(text)
        },
        uiState = uiState,
        onBack = onBack,
        onStockClick = onStockClick,
        onToggleWatchlist = viewModel::toggleWatchlist,
        onRetry = viewModel::retry,
    )

    // Runs once when the screen first appears (key = Unit never changes). After a rotation, the
    // restored text is sent to the ViewModel again so results match what's in the field.
    LaunchedEffect(Unit) { viewModel.onQueryChange(query) }
}

@Composable
fun SearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    uiState: UiState<List<SearchResult>>,
    onBack: () -> Unit,
    onStockClick: (String) -> Unit,
    onToggleWatchlist: (SearchResult) -> Unit,
    onRetry: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Scaffold(topBar = { PortfolioXTopBar(title = stringResource(R.string.action_search), onBack = onBack) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search,
                ),
            )

            when (uiState) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(uiState.messageRes, onRetry = onRetry)
                UiState.Empty -> EmptyView(
                    title = stringResource(R.string.search_no_results_title),
                    message = stringResource(R.string.search_no_results_message, query.trim()),
                )
                is UiState.Success -> SearchResults(uiState.data, onStockClick, onToggleWatchlist)
            }
        }
    }

    // Open the keyboard for a fresh search. When the user comes back from a stock with results
    // already showing, leave the keyboard closed so the results stay visible.
    LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }
}

@Composable
private fun SearchResults(
    results: List<SearchResult>,
    onStockClick: (String) -> Unit,
    onToggleWatchlist: (SearchResult) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results, key = { it.stock.symbol }) { result ->
            StockRow(
                symbol = result.stock.symbol,
                name = result.stock.name,
                price = result.quote?.price,
                change = result.quote?.change,
                changePercent = result.quote?.changePercent,
                onClick = { onStockClick(result.stock.symbol) },
                trailing = { WatchStar(isWatched = result.isWatched, onClick = { onToggleWatchlist(result) }) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun WatchStar(isWatched: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (isWatched) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.watchlist_remove),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(painterResource(R.drawable.ic_star_border), contentDescription = stringResource(R.string.watchlist_add))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    PortfolioXTheme {
        SearchContent(
            query = "a",
            onQueryChange = {},
            uiState = UiState.Success(
                listOf(
                    SearchResult(Stock("AAPL", "Apple Inc.", "NASDAQ", "Technology"), null, isWatched = true),
                    SearchResult(Stock("AMD", "Advanced Micro Devices, Inc.", "NASDAQ", "Semiconductors"), null, isWatched = false),
                ),
            ),
            onBack = {},
            onStockClick = {},
            onToggleWatchlist = {},
            onRetry = {},
        )
    }
}
