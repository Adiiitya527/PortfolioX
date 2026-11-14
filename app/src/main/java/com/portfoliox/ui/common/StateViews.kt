package com.portfoliox.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfoliox.R
import com.portfoliox.ui.theme.PortfolioXTheme

// Full-screen views for the non-success UiStates. Every screen uses these,
// so loading, empty and error look and behave the same across the app.

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * @param messageRes string resource id, as carried by UiState.Error.
 * @param onRetry when non-null, shows a "Try again" button. Nullable function types let the
 * caller decide whether an action exists, instead of passing a boolean plus a callback.
 */
@Composable
fun ErrorView(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    MessageView(
        title = stringResource(R.string.error_title),
        message = stringResource(messageRes),
        modifier = modifier,
    ) {
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

@Composable
fun EmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageView(title = title, message = message, modifier = modifier) {
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Shared layout for Error and Empty. The trailing `action` parameter is a composable lambda
 * (a "slot"): the caller passes in whatever UI goes there, like React's `children` prop.
 */
@Composable
private fun MessageView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        action()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorViewPreview() {
    PortfolioXTheme { ErrorView(messageRes = R.string.error_network, onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun EmptyViewPreview() {
    PortfolioXTheme {
        EmptyView(
            title = "Your watchlist is empty",
            message = "Search for stocks to follow their prices.",
            actionLabel = "Search stocks",
            onAction = {},
        )
    }
}
