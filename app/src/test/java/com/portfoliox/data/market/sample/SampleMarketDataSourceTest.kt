package com.portfoliox.data.market.sample

import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.domain.model.ChartRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * `runTest` runs suspend code in a test with virtual time: `delay(3_000)` completes instantly,
 * while `testScheduler.currentTime` still advances by 3 000 ms.
 */
class SampleMarketDataSourceTest {

    private val start: Instant = Instant.parse("2026-11-13T15:00:00Z")

    /** A source frozen at [start], with no artificial latency. */
    private val source = SampleMarketDataSource(clock = Clock.fixed(start, ZoneOffset.UTC), latencyMillis = 0)

    /** A Clock driven by the test's virtual time, so time moves forward as delays are skipped. */
    @OptIn(ExperimentalCoroutinesApi::class) // testScheduler.currentTime is marked experimental
    private fun TestScope.virtualClock(): Clock = object : Clock() {
        override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }

    // ---- search ----

    @Test
    fun `search matches symbols case-insensitively`() = runTest {
        val results = source.searchStocks("aapl")

        assertEquals("AAPL", results.first().symbol)
    }

    @Test
    fun `search matches company names`() = runTest {
        val results = source.searchStocks("coca")

        assertEquals(listOf("KO"), results.map { it.symbol })
    }

    @Test
    fun `search ranks symbol prefix matches first`() = runTest {
        val results = source.searchStocks("A")

        assertTrue(results.first().symbol.startsWith("A"))
        // "NVIDIA" contains an "a" but the symbol doesn't start with it, so it comes after AAPL, AMD, AMZN.
        val firstNonPrefix = results.indexOfFirst { !it.symbol.startsWith("A") }
        assertTrue(results.drop(firstNonPrefix).none { it.symbol.startsWith("A") })
    }

    @Test
    fun `blank search returns every stock sorted by symbol`() = runTest {
        val results = source.searchStocks("  ")

        assertEquals(SampleStocks.all.size, results.size)
        assertEquals(results.map { it.symbol }.sorted(), results.map { it.symbol })
    }

    @Test
    fun `search with no matches returns an empty list`() = runTest {
        assertTrue(source.searchStocks("zzzz").isEmpty())
    }

    // ---- quotes ----

    @Test
    fun `quote is deterministic and internally consistent`() = runTest {
        val first = source.getQuote("AAPL")
        val second = source.getQuote("aapl")

        assertEquals(first, second)
        assertTrue(first.dayHigh >= first.price && first.price >= first.dayLow)
        assertEquals(SampleStocks.all.first { it.stock.symbol == "AAPL" }.referencePrice, first.previousClose)
    }

    @Test
    fun `unknown symbol throws StockNotFoundException`() = runTest {
        // runCatching turns a thrown exception into a Result value we can inspect.
        val result = runCatching { source.getQuote("NOPE") }

        assertTrue(result.exceptionOrNull() is StockNotFoundException)
    }

    @Test
    fun `observeQuotes emits requested symbols on every tick`() = runTest {
        val live = SampleMarketDataSource(clock = virtualClock(), latencyMillis = 0, tickIntervalMillis = 3_000)

        val emissions = live.observeQuotes(setOf("AAPL", "MSFT", "NOPE")).take(3).toList()

        assertEquals(3, emissions.size)
        emissions.forEach { assertEquals(setOf("AAPL", "MSFT"), it.keys) } // unknown symbol left out
        val times = emissions.map { it.getValue("AAPL").timestamp }
        assertEquals(listOf(start, start.plusSeconds(3), start.plusSeconds(6)), times)
    }

    // ---- history ----

    @Test
    fun `history has one point per step and ends at the current price`() = runTest {
        val quote = source.getQuote("NVDA")

        // `for` over enum entries: check every range, not just one.
        for (range in ChartRange.entries) {
            val history = source.getPriceHistory("NVDA", range)

            assertEquals(range.pointCount, history.size)
            assertEquals(quote.price, history.last().close)
            assertEquals(start, history.last().timestamp)
            assertTrue(history.zipWithNext().all { (a, b) -> a.timestamp < b.timestamp })
            assertTrue(history.all { it.close.signum() > 0 })
        }
    }

    @Test
    fun `history is the same on every request`() = runTest {
        assertEquals(
            source.getPriceHistory("TSLA", ChartRange.YEAR),
            source.getPriceHistory("TSLA", ChartRange.YEAR),
        )
    }
}
