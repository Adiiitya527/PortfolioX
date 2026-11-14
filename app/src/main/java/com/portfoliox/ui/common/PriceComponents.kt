package com.portfoliox.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfoliox.core.Formatters
import com.portfoliox.ui.theme.PortfolioXTheme
import com.portfoliox.ui.theme.financialColors
import java.math.BigDecimal

/** Green for gains, red for losses, the normal text colour for zero. */
@Composable
fun changeColor(value: BigDecimal): Color = when (value.signum()) {
    1 -> MaterialTheme.financialColors.gain
    -1 -> MaterialTheme.financialColors.loss
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "+$12.30 (+1.23%)" coloured by direction. Used in rows, headers and the dashboard. */
@Composable
fun PriceChangeText(
    change: BigDecimal,
    changePercent: BigDecimal,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text = "${Formatters.signedCurrency(change)} (${Formatters.signedPercent(changePercent)})",
        color = changeColor(change),
        style = style,
        modifier = modifier,
    )
}

/**
 * One stock in a list: symbol and name on the left, price and change on the right.
 *
 * @param price null while the quote is loading; shows a dash instead.
 * @param trailing optional slot for an extra control, e.g. a watchlist star.
 */
@Composable
fun StockRow(
    symbol: String,
    name: String,
    price: BigDecimal?,
    change: BigDecimal?,
    changePercent: BigDecimal?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // weight(1f) makes this column take the remaining width, like flex: 1 in CSS.
        Column(modifier = Modifier.weight(1f)) {
            Text(text = symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text(
                text = price?.let(Formatters::currency) ?: "—",
                style = MaterialTheme.typography.titleMedium,
            )
            if (change != null && changePercent != null) {
                PriceChangeText(change = change, changePercent = changePercent, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StockRowPreview() {
    PortfolioXTheme {
        Column {
            StockRow("AAPL", "Apple Inc.", BigDecimal("231.45"), BigDecimal("2.31"), BigDecimal("1.01"), onClick = {})
            StockRow("TSLA", "Tesla, Inc.", BigDecimal("251.75"), BigDecimal("-4.20"), BigDecimal("-1.64"), onClick = {})
            StockRow("KO", "The Coca-Cola Company", null, null, null, onClick = {})
        }
    }
}
