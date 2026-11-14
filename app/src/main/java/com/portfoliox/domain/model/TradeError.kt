package com.portfoliox.domain.model

import java.math.BigDecimal

/**
 * Every reason a trade can be rejected.
 *
 * A sealed interface is a closed set of types: the compiler knows every implementation,
 * so a `when (error)` without an `else` branch fails to compile if a case is missed.
 * Web analogy: a TypeScript discriminated union.
 *
 * Cases with data are data classes; cases without data are `data object`s (singletons).
 * The UI turns these into user-facing text, so the domain layer holds no display strings.
 */
sealed interface TradeError {
    data object InvalidQuantity : TradeError

    data object PriceUnavailable : TradeError

    data class InsufficientCash(
        val required: BigDecimal,
        val available: BigDecimal,
    ) : TradeError

    data class InsufficientShares(
        val requested: Int,
        val owned: Int,
    ) : TradeError
}
