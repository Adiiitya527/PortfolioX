package com.portfoliox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.portfoliox.ui.dashboard.DashboardScreen
import com.portfoliox.ui.history.HistoryScreen
import com.portfoliox.ui.search.SearchScreen
import com.portfoliox.ui.stockdetail.StockDetailScreen
import com.portfoliox.ui.watchlist.WatchlistScreen

/**
 * The navigation graph: which composable is shown for each route.
 * Web analogy: the <Routes> table in React Router.
 *
 * Screens never receive the NavController. They get plain callbacks like `onStockClick`,
 * so they stay reusable and previewable, and all navigation decisions live in this one file.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = PortfolioRoute,
        modifier = modifier,
    ) {
        val openStock: (String) -> Unit = { symbol -> navController.navigate(StockDetailRoute(symbol)) }

        composable<PortfolioRoute> {
            DashboardScreen(onStockClick = openStock, onSearchClick = { navController.navigate(SearchRoute) })
        }
        composable<WatchlistRoute> {
            WatchlistScreen(onSearchClick = { navController.navigate(SearchRoute) }, onStockClick = openStock)
        }
        composable<HistoryRoute> {
            HistoryScreen(onStockClick = openStock, onSearchClick = { navController.navigate(SearchRoute) })
        }
        composable<SearchRoute> {
            SearchScreen(onBack = navController::navigateUp, onStockClick = openStock)
        }
        composable<StockDetailRoute> {
            // The symbol argument travels with the back stack entry. StockDetailViewModel's
            // factory reads it from the SavedStateHandle, so the screen needs no parameter for it.
            StockDetailScreen(onBack = navController::navigateUp)
        }
    }
}
