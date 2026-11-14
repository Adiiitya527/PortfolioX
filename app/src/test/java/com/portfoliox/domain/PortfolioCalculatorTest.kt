package com.portfoliox.domain

import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.assertMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Tests follow Arrange / Act / Assert. Backtick function names allow readable sentences,
 * which show up as-is in the test report.
 */
class PortfolioCalculatorTest {

    private fun holding(symbol: String = "AAPL", quantity: Int, averageCost: String) =
        Holding(symbol = symbol, quantity = quantity, averageCost = BigDecimal(averageCost))

    private fun quote(symbol: String, price: String, previousClose: String) = Quote(
        symbol = symbol,
        price = price.toMoney(),
        previousClose = previousClose.toMoney(),
        open = previousClose.toMoney(),
        dayHigh = price.toMoney(),
        dayLow = price.toMoney(),
        timestamp = Instant.EPOCH,
    )

    // ---- applyBuy ----

    @Test
    fun `first buy creates a holding at the purchase price`() {
        val result = PortfolioCalculator.applyBuy(existing = null, symbol = "AAPL", quantity = 10, price = "150.00".toMoney())

        assertEquals(10, result.quantity)
        assertMoney("150", result.averageCost)
    }

    @Test
    fun `second buy recalculates the weighted average cost`() {
        val existing = holding(quantity = 10, averageCost = "150")

        val result = PortfolioCalculator.applyBuy(existing, "AAPL", quantity = 5, price = "180.00".toMoney())

        // (10 × 150 + 5 × 180) / 15 = 2400 / 15 = 160
        assertEquals(15, result.quantity)
        assertMoney("160", result.averageCost)
    }

    @Test
    fun `average cost keeps four decimals so cost basis stays accurate`() {
        val existing = holding(quantity = 1, averageCost = "100")

        val result = PortfolioCalculator.applyBuy(existing, "AAPL", quantity = 2, price = "101.00".toMoney())

        // 302 / 3 = 100.6666... -> 100.6667; cost basis 100.6667 × 3 = 302.0001 -> 302.00
        assertMoney("100.6667", result.averageCost)
        assertMoney("302.00", result.costBasis)
    }

    @Test
    fun `buying zero shares is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            PortfolioCalculator.applyBuy(existing = null, symbol = "AAPL", quantity = 0, price = "150".toMoney())
        }
    }

    // ---- applySell ----

    @Test
    fun `partial sell keeps average cost and realizes profit`() {
        val existing = holding(quantity = 10, averageCost = "150")

        val result = PortfolioCalculator.applySell(existing, quantity = 4, price = "175.00".toMoney())

        assertEquals(6, result.remaining?.quantity)
        assertMoney("150", result.remaining?.averageCost)
        assertMoney("100.00", result.realizedPnl) // (175 − 150) × 4
    }

    @Test
    fun `selling every share removes the holding and can realize a loss`() {
        val existing = holding(quantity = 10, averageCost = "150")

        val result = PortfolioCalculator.applySell(existing, quantity = 10, price = "140.00".toMoney())

        assertNull(result.remaining)
        assertMoney("-100.00", result.realizedPnl)
    }

    @Test
    fun `selling more shares than owned is rejected`() {
        val existing = holding(quantity = 3, averageCost = "150")

        assertThrows(IllegalArgumentException::class.java) {
            PortfolioCalculator.applySell(existing, quantity = 4, price = "150".toMoney())
        }
    }

    // ---- valuate & summarize ----

    @Test
    fun `holding without a quote is valued at cost`() {
        val msft = holding(symbol = "MSFT", quantity = 2, averageCost = "400")

        val valuation = PortfolioCalculator.valuate(listOf(msft), quotes = emptyMap()).single()

        assertMoney("800.00", valuation.marketValue)
        assertMoney("0", valuation.unrealizedPnl)
    }

    @Test
    fun `summary adds up value, cost, profit and day change across holdings`() {
        val holdings = listOf(
            holding(symbol = "AAPL", quantity = 10, averageCost = "150"),
            holding(symbol = "MSFT", quantity = 2, averageCost = "400"),
        )
        val quotes = mapOf(
            "AAPL" to quote("AAPL", price = "160", previousClose = "155"), // value 1600, P&L +100, day +50
            "MSFT" to quote("MSFT", price = "380", previousClose = "390"), // value 760,  P&L −40,  day −20
        )

        val valuations = PortfolioCalculator.valuate(holdings, quotes)
        val summary = PortfolioCalculator.summarize(valuations, cash = "1000".toMoney())

        assertMoney("2300.00", summary.invested)
        assertMoney("2360.00", summary.holdingsValue)
        assertMoney("60.00", summary.unrealizedPnl)
        assertMoney("2.61", summary.unrealizedPnlPercent) // 60 / 2300
        assertMoney("30.00", summary.dayChange)
        assertMoney("1.29", summary.dayChangePercent) // 30 / (2360 − 30)
        assertMoney("3360.00", summary.totalValue)
    }

    @Test
    fun `empty portfolio summary is all zeros except cash`() {
        val summary = PortfolioCalculator.summarize(emptyList<HoldingValuation>(), cash = "100000".toMoney())

        assertMoney("0", summary.invested)
        assertMoney("0", summary.unrealizedPnl)
        assertMoney("0", summary.unrealizedPnlPercent)
        assertMoney("100000", summary.totalValue)
    }
}
