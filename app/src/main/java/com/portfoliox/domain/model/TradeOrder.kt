package com.portfoliox.domain.model

import java.math.BigDecimal

/**
 * A trade the user wants to place. It isn't validated yet (see TradeValidator),
 * so it may hold invalid input such as a zero quantity.
 */
data class TradeOrder(
    val symbol: String,
    val type: TradeType,
    val quantity: Int,
    val price: BigDecimal,
) {
    val total: BigDecimal
        get() = (price * quantity.toBigDecimal()).toMoney()
}
