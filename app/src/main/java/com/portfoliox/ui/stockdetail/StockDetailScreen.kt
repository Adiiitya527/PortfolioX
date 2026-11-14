package com.portfoliox.ui.stockdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.portfoliox.R
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.PortfolioXTopBar

/** Placeholder until Phase 5. Shows the symbol received through the typed route. */
@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { PortfolioXTopBar(title = symbol, onBack = onBack) }) { innerPadding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.stock_detail_placeholder, symbol),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
