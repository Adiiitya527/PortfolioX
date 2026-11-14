package com.portfoliox.domain.model

import java.math.BigDecimal
import java.time.Instant

/** One point on a historical price chart. */
data class PricePoint(
    val timestamp: Instant,
    val close: BigDecimal,
)
