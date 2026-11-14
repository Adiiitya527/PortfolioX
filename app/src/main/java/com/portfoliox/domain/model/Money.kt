package com.portfoliox.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rounding rules for money, kept in one place so every calculation rounds the same way.
 *
 * Why BigDecimal and not Double: Double is binary floating point, so 0.1 + 0.2 = 0.30000000000000004.
 * BigDecimal stores exact decimal digits, which is what money needs.
 */
object Money {
    /** Cash amounts, totals and P&L are stored to the cent. */
    const val SCALE = 2

    /** Average cost keeps extra precision so repeated buys don't accumulate rounding drift. */
    const val AVERAGE_COST_SCALE = 4

    /** Percentages are shown with two decimals, e.g. 12.34%. */
    const val PERCENT_SCALE = 2

    /**
     * Banker's rounding: .5 rounds to the nearest even digit (2.345 -> 2.34, 2.355 -> 2.36).
     * Over many operations it doesn't bias totals upwards the way HALF_UP does.
     */
    val ROUNDING: RoundingMode = RoundingMode.HALF_EVEN
}

/** Rounds to cents. Extension function: lets us write `amount.toMoney()` on any BigDecimal. */
fun BigDecimal.toMoney(): BigDecimal = setScale(Money.SCALE, Money.ROUNDING)

/** Parses a literal such as "189.50" into a money value. Handy for sample data and tests. */
fun String.toMoney(): BigDecimal = BigDecimal(this).toMoney()

/**
 * Percentage of [part] relative to [base], e.g. percentOf(25, 200) = 12.50.
 * Returns zero when [base] is zero instead of throwing ArithmeticException.
 *
 * Note: we call divide() with an explicit scale. Kotlin's `/` operator on BigDecimal keeps
 * the scale of the left-hand side, so `BigDecimal("1") / BigDecimal("3")` gives 0, not 0.33.
 */
fun percentOf(part: BigDecimal, base: BigDecimal): BigDecimal {
    if (base.signum() == 0) return BigDecimal.ZERO.setScale(Money.PERCENT_SCALE)
    return part.multiply(BigDecimal(100)).divide(base, Money.PERCENT_SCALE, Money.ROUNDING)
}
