package com.portfoliox.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns numbers and dates into display text. Kept out of composables so it can be unit tested
 * and so every screen formats money the same way.
 *
 * Amounts are USD and use Locale.US grouping ("1,234.56"). A real multi-currency app would
 * pass the currency and the user's locale in.
 *
 * Negative values use the typographic minus sign (−, U+2212), which lines up with "+"
 * and is read as "minus" by screen readers.
 */
object Formatters {

    private const val MINUS = "−"

    /** 1234.5 -> "$1,234.50", -12.3 -> "−$12.30" */
    fun currency(value: BigDecimal): String {
        val sign = if (value.signum() < 0) MINUS else ""
        // `\$` is a literal dollar sign; `${…}` inserts a value (string template).
        return "$sign\$${grouped(value.abs())}"
    }

    /** Always shows a sign for non-zero values: 12.3 -> "+$12.30", -12.3 -> "−$12.30", 0 -> "$0.00" */
    fun signedCurrency(value: BigDecimal): String =
        if (value.signum() > 0) "+${currency(value)}" else currency(value)

    /** 1.234 -> "+1.23%", -0.5 -> "−0.50%", 0 -> "0.00%" */
    fun signedPercent(value: BigDecimal): String {
        val sign = when (value.signum()) {
            1 -> "+"
            -1 -> MINUS
            else -> ""
        }
        return "$sign${value.abs().setScale(2, RoundingMode.HALF_EVEN).toPlainString()}%"
    }

    /** Plain price without a currency sign, for compact rows: 1234.5 -> "1,234.50" */
    fun price(value: BigDecimal): String = grouped(value)

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US)

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /** Time of day only: "15:30". */
    fun time(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter.format(instant.atZone(zone))

    /** A calendar date: "14 Nov 2026". */
    fun date(date: LocalDate): String = dateFormatter.format(date)

    /** Formats an instant in the device's time zone: "14 Nov 2026, 15:30". */
    fun dateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(instant.atZone(zone))

    /** Date only: "14 Nov 2026". */
    fun date(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(instant.atZone(zone))

    // String.format with %,.2f handles BigDecimal exactly (no conversion to Double).
    private fun grouped(value: BigDecimal): String =
        String.format(Locale.US, "%,.2f", value.setScale(2, RoundingMode.HALF_EVEN))
}
