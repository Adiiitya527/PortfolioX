package com.portfoliox.domain.model

import java.math.BigDecimal

/**
 * A [Holding] combined with current market prices: what it's worth now and how it's doing.
 * This is what a row in the holdings list displays.
 */
data class HoldingValuation(
    val holding: Holding,
    val currentPrice: BigDecimal,
    val previousClose: BigDecimal,
) {
    private val quantity: BigDecimal
        get() = holding.quantity.toBigDecimal()

    val marketValue: BigDecimal
        get() = (currentPrice * quantity).toMoney()

    /** Profit or loss on paper: the value now minus what was paid. */
    val unrealizedPnl: BigDecimal
        get() = marketValue - holding.costBasis

    val unrealizedPnlPercent: BigDecimal
        get() = percentOf(unrealizedPnl, holding.costBasis)

    /** How much the position moved today. */
    val dayChange: BigDecimal
        get() = ((currentPrice - previousClose) * quantity).toMoney()

    // A companion object holds functions that belong to the class rather than an instance,
    // like `static` methods in Java. These are factory functions: HoldingValuation.from(...).
    companion object {
        fun from(holding: Holding, quote: Quote): HoldingValuation =
            HoldingValuation(holding, currentPrice = quote.price, previousClose = quote.previousClose)

        /**
         * Used when no price is available yet (for example, before the first quote arrives).
         * Valuing at cost shows zero P&L instead of wrong numbers.
         */
        fun atCost(holding: Holding): HoldingValuation =
            HoldingValuation(holding, currentPrice = holding.averageCost, previousClose = holding.averageCost)
    }
}
