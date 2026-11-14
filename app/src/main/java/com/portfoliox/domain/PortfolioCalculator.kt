package com.portfoliox.domain

import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.Money
import com.portfoliox.domain.model.PortfolioSummary
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.percentOf
import com.portfoliox.domain.model.toMoney
import java.math.BigDecimal

/** Result of selling shares: what's left of the holding (null if everything was sold) and the profit or loss. */
data class SellResult(
    val remaining: Holding?,
    val realizedPnl: BigDecimal,
)

/**
 * Portfolio maths as pure functions: same input, same output, no I/O and no hidden state.
 * That makes it the easiest part of the app to unit test, and the part where bugs cost the most.
 *
 * `object` declares a singleton. There's no state to configure, so nothing needs to be constructed or injected.
 */
object PortfolioCalculator {

    /**
     * Adds bought shares to a holding and recalculates the weighted average cost:
     * newAverage = (oldQty × oldAverage + boughtQty × price) / (oldQty + boughtQty)
     */
    fun applyBuy(existing: Holding?, symbol: String, quantity: Int, price: BigDecimal): Holding {
        require(quantity > 0) { "Buy quantity must be positive" }
        require(existing == null || existing.symbol == symbol) { "Holding symbol mismatch" }

        // `?:` (Elvis operator): use the left side if it isn't null, otherwise the right side.
        val oldQuantity = existing?.quantity ?: 0
        val oldCost = existing?.let { it.averageCost * it.quantity.toBigDecimal() } ?: BigDecimal.ZERO

        val newQuantity = oldQuantity + quantity
        val newCost = oldCost + price * quantity.toBigDecimal()
        val newAverage = newCost.divide(newQuantity.toBigDecimal(), Money.AVERAGE_COST_SCALE, Money.ROUNDING)

        return Holding(symbol = symbol, quantity = newQuantity, averageCost = newAverage)
    }

    /**
     * Removes sold shares from a holding. The average cost of the remaining shares doesn't change.
     * Profit or loss is locked in ("realized") on the sold shares: (price − averageCost) × quantity.
     */
    fun applySell(existing: Holding, quantity: Int, price: BigDecimal): SellResult {
        require(quantity in 1..existing.quantity) {
            "Sell quantity must be between 1 and ${existing.quantity}, was $quantity"
        }

        val realizedPnl = ((price - existing.averageCost) * quantity.toBigDecimal()).toMoney()
        val remainingQuantity = existing.quantity - quantity
        // `copy` creates a new Holding with one field changed; the original is untouched.
        val remaining = if (remainingQuantity == 0) null else existing.copy(quantity = remainingQuantity)

        return SellResult(remaining = remaining, realizedPnl = realizedPnl)
    }

    /** Pairs each holding with its latest quote. Holdings without a quote yet are valued at cost. */
    fun valuate(holdings: List<Holding>, quotes: Map<String, Quote>): List<HoldingValuation> =
        holdings.map { holding ->
            val quote = quotes[holding.symbol]
            if (quote != null) HoldingValuation.from(holding, quote) else HoldingValuation.atCost(holding)
        }

    /** Adds up every holding into the dashboard totals. */
    fun summarize(valuations: List<HoldingValuation>, cash: BigDecimal): PortfolioSummary {
        // sumOf with a BigDecimal selector adds exactly, with no conversion to Double.
        val invested = valuations.sumOf { it.holding.costBasis }
        val holdingsValue = valuations.sumOf { it.marketValue }
        val dayChange = valuations.sumOf { it.dayChange }
        val unrealizedPnl = holdingsValue - invested

        // Today's percentage is measured against yesterday's value, not today's.
        val valueAtPreviousClose = holdingsValue - dayChange

        return PortfolioSummary(
            cash = cash.toMoney(),
            invested = invested.toMoney(),
            holdingsValue = holdingsValue.toMoney(),
            unrealizedPnl = unrealizedPnl.toMoney(),
            unrealizedPnlPercent = percentOf(unrealizedPnl, invested),
            dayChange = dayChange.toMoney(),
            dayChangePercent = percentOf(dayChange, valueAtPreviousClose),
        )
    }
}
