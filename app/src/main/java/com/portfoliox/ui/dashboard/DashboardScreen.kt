package com.portfoliox.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfoliox.R
import com.portfoliox.core.Formatters
import com.portfoliox.ui.common.EmptyView
import com.portfoliox.ui.common.ErrorView
import com.portfoliox.ui.common.LoadingView
import com.portfoliox.ui.common.PortfolioXTopBar
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.theme.PortfolioXTheme
import java.math.BigDecimal

/**
 * Stateful entry point: obtains the ViewModel and collects its state.
 *
 * viewModel(factory = …) returns the same instance for as long as this screen is on the back
 * stack, including across rotation.
 *
 * collectAsStateWithLifecycle stops collecting when the app goes to the background and resumes
 * when it returns. A plain collectAsState would keep collecting.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState = uiState)
}

/**
 * Stateless content: depends only on its parameters, so it can be previewed and tested with any
 * state. This split is called "state hoisting". Like a presentational React component
 * that receives props from a container.
 */
@Composable
fun DashboardContent(uiState: UiState<BigDecimal>) {
    Scaffold(topBar = { PortfolioXTopBar(title = stringResource(R.string.nav_portfolio)) }) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        // `when` over the sealed UiState: the compiler checks every state is handled.
        when (uiState) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(messageRes = uiState.messageRes, modifier = contentModifier)
            UiState.Empty -> EmptyView(
                title = stringResource(R.string.placeholder_title),
                message = stringResource(R.string.portfolio_coming_soon),
                modifier = contentModifier,
            )
            is UiState.Success -> CashCard(cash = uiState.data, modifier = contentModifier)
        }
    }
}

@Composable
private fun CashCard(cash: BigDecimal, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.portfolio_cash_available),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = Formatters.currency(cash), style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            text = stringResource(R.string.portfolio_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardSuccessPreview() {
    PortfolioXTheme { DashboardContent(UiState.Success(BigDecimal("100000.00"))) }
}

@Preview(showBackground = true)
@Composable
private fun DashboardErrorPreview() {
    PortfolioXTheme { DashboardContent(UiState.Error(R.string.error_generic)) }
}
