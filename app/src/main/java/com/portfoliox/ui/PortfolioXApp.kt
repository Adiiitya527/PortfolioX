package com.portfoliox.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.portfoliox.ui.navigation.AppNavHost
import com.portfoliox.ui.navigation.TopLevelDestination

/**
 * The root composable: bottom navigation bar plus the navigation host.
 *
 * Insets (system bar space): this outer Scaffold handles only the bottom bar. Each screen's
 * own Scaffold handles the status bar with its top app bar. consumeWindowInsets tells inner
 * screens that the bottom space is already taken, so it isn't padded twice.
 */
@Composable
fun PortfolioXApp() {
    // rememberNavController keeps one NavController across recompositions.
    val navController = rememberNavController()

    // `by` delegates to a State object: reading `backStackEntry` subscribes this composable
    // to changes, so it recomposes whenever the user navigates.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isOn(it) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination.isOn(destination),
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

/** True when this destination, or one of its parents, is the given tab's route. */
private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/**
 * Standard tab switching:
 * - popUpTo the start destination, so tabs don't pile up on the back stack
 * - saveState / restoreState, so each tab keeps its scroll position and sub-screens
 * - launchSingleTop, so tapping the current tab doesn't open a second copy
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
