package com.portfoliox.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.portfoliox.R
import kotlinx.serialization.Serializable

// Type-safe routes. Each screen's address is a Kotlin type instead of a string like
// "stock/{symbol}". Arguments are constructor properties, so a missing or misspelled
// argument is a compile error. @Serializable lets Navigation encode them into the back stack.
// Web analogy: typed route definitions in TanStack Router instead of string paths.

@Serializable
data object PortfolioRoute

@Serializable
data object WatchlistRoute

@Serializable
data object HistoryRoute

@Serializable
data object SearchRoute

@Serializable
data class StockDetailRoute(val symbol: String)

/** The tabs in the bottom navigation bar. Iterating `entries` builds the bar. */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    PORTFOLIO(PortfolioRoute, R.string.nav_portfolio, Icons.Filled.Home),
    WATCHLIST(WatchlistRoute, R.string.nav_watchlist, Icons.Filled.Star),
    HISTORY(HistoryRoute, R.string.nav_history, Icons.AutoMirrored.Filled.List),
}
