package com.portfoliox.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

class FormattersTest {

    private val minus = "−"

    @Test
    fun `currency groups thousands and always shows two decimals`() {
        assertEquals("$1,234.50", Formatters.currency(BigDecimal("1234.5")))
        assertEquals("$100,000.00", Formatters.currency(BigDecimal("100000")))
        assertEquals("$0.00", Formatters.currency(BigDecimal.ZERO))
    }

    @Test
    fun `negative currency puts the minus sign before the dollar sign`() {
        assertEquals("${minus}$12.30", Formatters.currency(BigDecimal("-12.3")))
    }

    @Test
    fun `signed currency shows plus for gains and nothing for zero`() {
        assertEquals("+$12.30", Formatters.signedCurrency(BigDecimal("12.3")))
        assertEquals("${minus}$12.30", Formatters.signedCurrency(BigDecimal("-12.3")))
        assertEquals("$0.00", Formatters.signedCurrency(BigDecimal.ZERO))
    }

    @Test
    fun `signed percent rounds to two decimals`() {
        assertEquals("+1.23%", Formatters.signedPercent(BigDecimal("1.234")))
        assertEquals("${minus}0.50%", Formatters.signedPercent(BigDecimal("-0.5")))
        assertEquals("0.00%", Formatters.signedPercent(BigDecimal.ZERO))
    }

    @Test
    fun `date time is formatted in the given zone`() {
        val instant = Instant.parse("2026-11-14T10:00:00Z")

        assertEquals("14 Nov 2026, 15:30", Formatters.dateTime(instant, ZoneOffset.ofHoursMinutes(5, 30)))
        assertEquals("14 Nov 2026", Formatters.date(instant, ZoneOffset.UTC))
        assertEquals("15:30", Formatters.time(instant, ZoneOffset.ofHoursMinutes(5, 30)))
        assertEquals("14 Nov 2026", Formatters.date(java.time.LocalDate.of(2026, 11, 14)))
    }
}
