package com.portfoliox.ui.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfoliox.R
import com.portfoliox.core.Formatters
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeType
import com.portfoliox.ui.theme.PortfolioXTheme
import java.math.BigDecimal
import java.time.Instant

/**
 * A modal bottom sheet: slides up over the current screen and dims it, like a mobile-friendly
 * modal dialog. Dismissed by swiping down, tapping outside, or pressing Back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    state: TradeUiState,
    onDismiss: () -> Unit,
    onTypeChange: (TradeType) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUseMax: () -> Unit,
    onSubmit: () -> Unit,
) {
    // skipPartiallyExpanded: open fully straight away; this form is short and needs the keyboard.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        TradeForm(state, onTypeChange, onQuantityChange, onUseMax, onSubmit)
    }
}

/** The sheet's content, separate from the sheet so it can be previewed on its own. */
@Composable
fun TradeForm(
    state: TradeUiState,
    onTypeChange: (TradeType) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUseMax: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // The user opened the sheet to enter a quantity, so put the cursor there straight away.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // imePadding: leave room for the on-screen keyboard. verticalScroll: in landscape the form is
    // taller than the space above the keyboard, so it must scroll to reach the quantity field.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(
                if (state.type == TradeType.BUY) R.string.trade_title_buy else R.string.trade_title_sell,
                state.symbol,
            ),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))

        TypeSelector(selected = state.type, onTypeChange = onTypeChange)
        Spacer(Modifier.height(16.dp))

        InfoRow(stringResource(R.string.trade_market_price), state.quote?.price?.let(Formatters::currency) ?: "—")
        if (state.type == TradeType.BUY) {
            InfoRow(stringResource(R.string.trade_cash_available), Formatters.currency(state.cash))
        } else {
            InfoRow(stringResource(R.string.trade_shares_owned), state.ownedQuantity.toString())
        }
        Spacer(Modifier.height(12.dp))

        val problem = state.validationError ?: state.rejection
        OutlinedTextField(
            value = state.quantityText,
            onValueChange = onQuantityChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.trade_quantity)) },
            singleLine = true,
            isError = problem != null || state.failureRes != null,
            supportingText = {
                when {
                    problem != null -> Text(tradeErrorMessage(problem))
                    state.failureRes != null -> Text(stringResource(state.failureRes))
                }
            },
            trailingIcon = {
                TextButton(onClick = onUseMax, enabled = state.maxQuantity > 0) {
                    Text(stringResource(R.string.trade_max))
                }
            },
            // Number keyboard; "Done" on the keyboard submits when the order is valid.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onSubmit() }),
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        InfoRow(
            label = stringResource(R.string.trade_estimated_total),
            value = state.estimatedTotal?.let(Formatters::currency) ?: "—",
            emphasized = true,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(confirmLabel(state))
            }
        }

        Text(
            text = stringResource(R.string.trade_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun TypeSelector(selected: TradeType, onTypeChange: (TradeType) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TradeType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onTypeChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index, TradeType.entries.size),
            ) {
                Text(stringResource(if (type == TradeType.BUY) R.string.trade_buy else R.string.trade_sell))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = style)
    }
}

/** "Buy 10 shares" / "Sell 1 share": plurals pick the right word form for the count. */
@Composable
private fun confirmLabel(state: TradeUiState): String {
    val shares = state.quantity ?: 0
    return when {
        shares <= 0 -> stringResource(if (state.type == TradeType.BUY) R.string.trade_buy else R.string.trade_sell)
        state.type == TradeType.BUY -> pluralStringResource(R.plurals.trade_confirm_buy, shares, shares)
        else -> pluralStringResource(R.plurals.trade_confirm_sell, shares, shares)
    }
}

/** Turns a domain TradeError into text. The domain layer stays free of UI strings. */
@Composable
fun tradeErrorMessage(error: TradeError): String = when (error) {
    TradeError.InvalidQuantity -> stringResource(R.string.trade_error_invalid_quantity)
    TradeError.PriceUnavailable -> stringResource(R.string.trade_error_price_unavailable)
    is TradeError.InsufficientCash -> stringResource(
        R.string.trade_error_insufficient_cash,
        Formatters.currency(error.required),
        Formatters.currency(error.available),
    )
    is TradeError.InsufficientShares ->
        if (error.owned == 0) {
            stringResource(R.string.trade_error_no_shares)
        } else {
            pluralStringResource(R.plurals.trade_error_insufficient_shares, error.owned, error.owned)
        }
}

@Preview(showBackground = true)
@Composable
private fun TradeFormPreview() {
    val quote = Quote("IBKR", BigDecimal("188.40"), BigDecimal("187.55"), BigDecimal("187.90"), BigDecimal("189.10"), BigDecimal("186.80"), Instant.EPOCH)
    PortfolioXTheme {
        TradeForm(
            state = TradeUiState(symbol = "IBKR", quantityText = "600", quote = quote, cash = BigDecimal("100000.00")),
            onTypeChange = {},
            onQuantityChange = {},
            onUseMax = {},
            onSubmit = {},
        )
    }
}
