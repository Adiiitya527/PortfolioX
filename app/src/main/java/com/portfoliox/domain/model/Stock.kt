package com.portfoliox.domain.model

/**
 * A tradable company. Static information that rarely changes, unlike its price (see [Quote]).
 *
 * `data class` generates equals/hashCode/toString/copy, so two Stocks with the same values
 * are equal. Every property is `val` (read-only): models are immutable, and changes create new copies.
 */
data class Stock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val sector: String,
)
