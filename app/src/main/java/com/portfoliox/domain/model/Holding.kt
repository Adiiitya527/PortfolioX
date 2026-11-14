package com.portfoliox.domain.model

import java.math.BigDecimal

/**
 * Shares the user currently owns in one stock.
 *
 * The `init` block runs on construction and enforces the invariants: a holding always has
 * at least one share. When the last share is sold the holding is deleted, never kept at zero.
 * `require` throws IllegalArgumentException if the condition is false (like a Python assert, but always on).
 */
data class Holding(
    val symbol: String,
    val quantity: Int,
    val averageCost: BigDecimal,
) {
    init {
        require(quantity > 0) { "Holding quantity must be positive, was $quantity" }
        require(averageCost.signum() >= 0) { "Average cost cannot be negative, was $averageCost" }
    }

    /** Total amount paid for the shares still held. */
    val costBasis: BigDecimal
        get() = (averageCost * quantity.toBigDecimal()).toMoney()
}
