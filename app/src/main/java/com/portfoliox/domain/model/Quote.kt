package com.portfoliox.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * A price snapshot for one stock at one moment.
 *
 * [change] and [changePercent] are computed properties (custom getters). They are derived
 * from the stored fields every time they are read, so they can never get out of sync.
 * Computed properties declared in the class body are not part of equals/hashCode.
 */
data class Quote(
    val symbol: String,
    val price: BigDecimal,
    val previousClose: BigDecimal,
    val open: BigDecimal,
    val dayHigh: BigDecimal,
    val dayLow: BigDecimal,
    val timestamp: Instant,
) {
    /** Price movement since yesterday's close. Kotlin's `-` calls BigDecimal.subtract(). */
    val change: BigDecimal
        get() = (price - previousClose).toMoney()

    val changePercent: BigDecimal
        get() = percentOf(price - previousClose, previousClose)
}
