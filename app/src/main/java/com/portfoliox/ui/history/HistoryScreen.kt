package com.portfoliox.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.portfoliox.R
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.PortfolioXTopBar

/** Placeholder until Phase 8. */
@Composable
fun HistoryScreen() {
    Scaffold(topBar = { PortfolioXTopBar(title = stringResource(R.string.nav_history)) }) { innerPadding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.history_placeholder),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
