package com.portfoliox.ui.stockdetail

import com.portfoliox.domain.model.PricePoint
import com.portfoliox.testutil.assertMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ChartStateTest {

    private fun points(vararg closes: String) =
        closes.mapIndexed { i, close -> PricePoint(Instant.ofEpochSecond(i.toLong()), BigDecimal(close)) }

    @Test
    fun `values are scaled between 0 for the lowest and 1 for the highest`() {
        assertEquals(listOf(0f, 0.5f, 1f), normalizeForChart(listOf(100f, 150f, 200f)))
    }

    @Test
    fun `a flat line is drawn in the middle instead of dividing by zero`() {
        assertEquals(listOf(0.5f, 0.5f), normalizeForChart(listOf(42f, 42f)))
    }

    @Test
    fun `touch position maps to the nearest point and is clamped to the chart`() {
        assertEquals(0, pointIndexAt(x = 0f, width = 1000f, pointCount = 11))
        assertEquals(5, pointIndexAt(x = 520f, width = 1000f, pointCount = 11))
        assertEquals(10, pointIndexAt(x = 1000f, width = 1000f, pointCount = 11))
        assertEquals(10, pointIndexAt(x = 5000f, width = 1000f, pointCount = 11)) // dragged past the edge
        assertEquals(0, pointIndexAt(x = -20f, width = 1000f, pointCount = 11))
    }

    @Test
    fun `range change compares the first and last points`() {
        val change = RangeChange.of(points("200.00", "190.00", "210.00"))

        assertMoney("10.00", change?.change)
        assertMoney("5.00", change?.changePercent)
    }

    @Test
    fun `range change needs at least two points`() {
        assertNull(RangeChange.of(points("100.00")))
    }
}
