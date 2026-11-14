package com.portfoliox.domain.model

import java.math.BigDecimal

/** The numbers shown on the dashboard summary card. */
data class PortfolioSummary(
    /** Uninvested money available for buying. */
    val cash: BigDecimal,
    /** Amount paid for the shares currently held (sum of cost bases). */
    val invested: BigDecimal,
    /** Current market value of all holdings. */
    val holdingsValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val unrealizedPnlPercent: BigDecimal,
    val dayChange: BigDecimal,
    val dayChangePercent: BigDecimal,
) {
    /** Cash plus holdings: the whole account's worth. */
    val totalValue: BigDecimal
        get() = cash + holdingsValue
}
