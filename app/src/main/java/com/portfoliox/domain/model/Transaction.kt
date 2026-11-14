package com.portfoliox.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * A completed trade, as shown in the transaction history. Transactions are never edited.
 *
 * [realizedPnl] is nullable (`BigDecimal?`): only sells lock in a profit or loss, so it's
 * null for buys. Kotlin's type system forces callers to handle the null case.
 */
data class Transaction(
    val id: Long,
    val symbol: String,
    val type: TradeType,
    val quantity: Int,
    val price: BigDecimal,
    val total: BigDecimal,
    val realizedPnl: BigDecimal?,
    val timestamp: Instant,
)
