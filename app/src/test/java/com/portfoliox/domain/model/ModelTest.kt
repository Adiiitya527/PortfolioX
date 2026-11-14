package com.portfoliox.domain.model

import com.portfoliox.testutil.assertMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ModelTest {

    private fun quote(price: String, previousClose: String) = Quote(
        symbol = "AAPL",
        price = price.toMoney(),
        previousClose = previousClose.toMoney(),
        open = previousClose.toMoney(),
        dayHigh = price.toMoney(),
        dayLow = previousClose.toMoney(),
        timestamp = Instant.EPOCH,
    )

    @Test
    fun `quote change and percent are relative to previous close`() {
        val quote = quote(price = "105.00", previousClose = "100.00")

        assertMoney("5.00", quote.change)
        assertMoney("5.00", quote.changePercent)
    }

    @Test
    fun `quote with falling price has negative change`() {
        val quote = quote(price = "97.50", previousClose = "100.00")

        assertMoney("-2.50", quote.change)
        assertMoney("-2.50", quote.changePercent)
    }

    @Test
    fun `percentOf rounds to two decimals and handles a zero base`() {
        assertMoney("33.33", percentOf(BigDecimal.ONE, BigDecimal(3)))
        assertMoney("0", percentOf(BigDecimal.TEN, BigDecimal.ZERO))
    }

    @Test
    fun `toMoney uses banker's rounding`() {
        assertEquals(BigDecimal("2.34"), BigDecimal("2.345").toMoney())
        assertEquals(BigDecimal("2.36"), BigDecimal("2.355").toMoney())
    }

    @Test
    fun `holding cannot have zero or negative quantity`() {
        assertThrows(IllegalArgumentException::class.java) {
            Holding(symbol = "AAPL", quantity = 0, averageCost = "100".toMoney())
        }
    }

    @Test
    fun `chart range point count includes both ends`() {
        assertEquals(85, ChartRange.WEEK.pointCount) // 7 days every 2 hours = 84 steps + 1
        assertEquals(91, ChartRange.QUARTER.pointCount)
    }
}
