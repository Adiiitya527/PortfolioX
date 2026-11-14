package com.portfoliox.domain

import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import java.math.BigDecimal

/**
 * Checks whether a trade can be placed, before anything is written to the database.
 *
 * Returns null when the order is valid, otherwise the first problem found. A nullable return
 * keeps the happy path simple: `val error = validate(...) ?: return execute(order)`.
 */
object TradeValidator {

    fun validate(order: TradeOrder, availableCash: BigDecimal, ownedQuantity: Int): TradeError? =
        // `when` without an argument works like an if/else-if chain and is itself an expression.
        when {
            order.quantity <= 0 -> TradeError.InvalidQuantity
            order.price.signum() <= 0 -> TradeError.PriceUnavailable
            order.type == TradeType.BUY && order.total > availableCash ->
                TradeError.InsufficientCash(required = order.total, available = availableCash)
            order.type == TradeType.SELL && order.quantity > ownedQuantity ->
                TradeError.InsufficientShares(requested = order.quantity, owned = ownedQuantity)
            else -> null
        }
}
