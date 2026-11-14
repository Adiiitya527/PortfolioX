package com.portfoliox.ui.watchlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.portfoliox.R
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.PortfolioXTopBar

/** Placeholder until Phase 4. Demonstrates a top-bar action that navigates to Search. */
@Composable
fun WatchlistScreen(onSearchClick: () -> Unit) {
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
    ) { innerPadding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.watchlist_placeholder),
            actionLabel = stringResource(R.string.action_search),
            onAction = onSearchClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
