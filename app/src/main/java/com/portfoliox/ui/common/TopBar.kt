package com.portfoliox.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.portfoliox.R

/**
 * The app's standard top bar.
 *
 * @param onBack when non-null, shows a back arrow (for screens that aren't tabs).
 * @param actions icons on the right. `RowScope.() -> Unit` is a lambda with a receiver:
 * inside it, `this` is a RowScope, so Row-only modifiers are available. Same idea as
 * Kotlin's `apply { }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioXTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
    )
}
