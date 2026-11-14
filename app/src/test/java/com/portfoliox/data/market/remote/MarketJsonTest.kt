package com.portfoliox.data.market.remote

import com.portfoliox.data.market.remote.dto.HistoryDto
import com.portfoliox.data.market.remote.dto.QuoteDto
import com.portfoliox.data.market.remote.dto.StockDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Parses JSON exactly as the FastAPI server sends it (copied from a real response), so a
 * renamed field on either side shows up as a failing test instead of a crash in the app.
 */
class MarketJsonTest {

    @Test
    fun `quote JSON maps to an exact domain quote`() {
        val json = """
            [{"symbol":"AAPL","price":"231.07","previous_close":"231.45","open":"232.22",
              "day_high":"233.09","day_low":"230.20","timestamp":"2026-11-14T15:09:15.326577Z"}]
        """.trimIndent()

        val quote = MarketJson.decodeFromString<List<QuoteDto>>(json).single().toDomain()

        assertEquals(BigDecimal("231.07"), quote.price)
        assertEquals(BigDecimal("231.45"), quote.previousClose)
        assertEquals(Instant.parse("2026-11-14T15:09:15.326577Z"), quote.timestamp)
    }

    @Test
    fun `unknown fields are ignored so the server can add new ones safely`() {
        val json = """{"symbol":"KO","name":"The Coca-Cola Company","exchange":"NYSE","sector":"Consumer Staples","currency":"USD"}"""

        val stock = MarketJson.decodeFromString<StockDto>(json).toDomain()

        assertEquals("KO", stock.symbol)
    }

    @Test
    fun `history JSON maps every point, oldest first`() {
        val json = """
            {"symbol":"KO","range":"1M","points":[
              {"timestamp":"2026-10-15T15:09:15.343640Z","close":"71.02"},
              {"timestamp":"2026-10-15T23:09:15.343640Z","close":"71.44"}]}
        """.trimIndent()

        val points = MarketJson.decodeFromString<HistoryDto>(json).points.map { it.toDomain() }

        assertEquals(listOf(BigDecimal("71.02"), BigDecimal("71.44")), points.map { it.close })
    }

    @Test
    fun `timestamps with a numeric UTC offset are accepted too`() {
        assertEquals(Instant.parse("2026-11-14T10:00:00Z"), parseTimestamp("2026-11-14T10:00:00+00:00"))
        assertEquals(Instant.parse("2026-11-14T04:30:00Z"), parseTimestamp("2026-11-14T10:00:00+05:30"))
    }
}
