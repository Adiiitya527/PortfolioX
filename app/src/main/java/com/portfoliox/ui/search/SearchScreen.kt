package com.portfoliox.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.portfoliox.R
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.StockRow

/** Placeholder until Phase 4. Two fixed rows demonstrate navigating with an argument. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
) {
    Scaffold(topBar = { PortfolioXTopBar(title = stringResource(R.string.action_search), onBack = onBack) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            StockRow("AAPL", "Apple Inc.", null, null, null, onClick = { onStockClick("AAPL") })
            HorizontalDivider()
            StockRow("IBKR", "Interactive Brokers Group, Inc.", null, null, null, onClick = { onStockClick("IBKR") })
        }
    }
}
