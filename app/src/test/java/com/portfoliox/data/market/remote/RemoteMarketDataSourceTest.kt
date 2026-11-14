package com.portfoliox.data.market.remote

import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.data.market.remote.dto.HistoryDto
import com.portfoliox.data.market.remote.dto.PricePointDto
import com.portfoliox.data.market.remote.dto.QuoteDto
import com.portfoliox.data.market.remote.dto.StockDto
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.testutil.assertMoney
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * MarketApi is an interface, so tests use a hand-written fake instead of a real server.
 * Each test scripts what the "server" returns or throws.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteMarketDataSourceTest {

    private class FakeMarketApi : MarketApi {
        val quoteResponses = ArrayDeque<() -> List<QuoteDto>>()
        var quoteCalls = 0
        var stockError: Throwable? = null
        var lastHistoryRange: String? = null
        var lastSearchQuery: String? = null

        override suspend fun searchStocks(query: String): List<StockDto> {
            lastSearchQuery = query
            return listOf(StockDto("AAPL", "Apple Inc.", "NASDAQ", "Technology"))
        }

        override suspend fun getStock(symbol: String): StockDto {
            stockError?.let { throw it }
            return StockDto(symbol, "Name", "NASDAQ", "Technology")
        }

        override suspend fun getQuotes(symbols: String): List<QuoteDto> {
            quoteCalls++
            return (quoteResponses.removeFirstOrNull() ?: { listOf(quote("AAPL", "100.00")) })()
        }

        override suspend fun getHistory(symbol: String, range: String): HistoryDto {
            stockError?.let { throw it }
            lastHistoryRange = range
            return HistoryDto(
                symbol,
                range,
                listOf(PricePointDto("2026-11-13T10:00:00Z", "99.50"), PricePointDto("2026-11-14T10:00:00Z", "100.25")),
            )
        }
    }

    private val api = FakeMarketApi()
    private val source = RemoteMarketDataSource(api, pollIntervalMillis = 3_000)

    private fun httpError(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

    @Test
    fun `404 from the server becomes StockNotFoundException`() = runTest {
        api.stockError = httpError(404)

        val result = runCatching { source.getStock("NOPE") }

        assertTrue(result.exceptionOrNull() is StockNotFoundException)
    }

    @Test
    fun `other HTTP errors are passed through unchanged`() = runTest {
        api.stockError = httpError(500)

        val result = runCatching { source.getStock("AAPL") }

        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `search trims the query and maps the results`() = runTest {
        val stocks = source.searchStocks("  apple ")

        assertEquals("apple", api.lastSearchQuery)
        assertEquals("Apple Inc.", stocks.single().name)
    }

    @Test
    fun `history requests the range label and maps points`() = runTest {
        val points = source.getPriceHistory("AAPL", ChartRange.QUARTER)

        assertEquals("3M", api.lastHistoryRange)
        assertMoney("100.25", points.last().close)
    }

    @Test
    fun `quotes are polled at the configured interval`() = runTest {
        val emissions = source.observeQuotes(setOf("AAPL")).take(3).toList()

        assertEquals(3, emissions.size)
        assertEquals(3, api.quoteCalls)
        assertEquals(6_000L, currentTime) // two waits of 3 s between three polls (virtual time)
    }

    @Test
    fun `a failed poll is retried with backoff and prices resume`() = runTest {
        api.quoteResponses += { throw IOException("connection refused") }
        api.quoteResponses += { throw IOException("connection refused") }
        api.quoteResponses += { listOf(quote("AAPL", "101.00")) }

        val first = source.observeQuotes(setOf("AAPL")).take(1).toList().single()

        assertMoney("101.00", first.getValue("AAPL").price)
        assertEquals(3_000L, currentTime) // waited 1 s, then 2 s
    }

    @Test
    fun `a non-transient error ends the stream so the screen can show it`() = runTest {
        api.quoteResponses += { throw httpError(400) }

        val result = runCatching { source.observeQuotes(setOf("AAPL")).take(1).toList() }

        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `no symbols means no network calls`() = runTest {
        val emissions = source.observeQuotes(emptySet()).toList()

        assertEquals(listOf(emptyMap<String, Any>()), emissions)
        assertEquals(0, api.quoteCalls)
    }

    @Test
    fun `backoff doubles from 1 second and is capped at 30 seconds`() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), (1..7).map(source::backoffMillis))
    }

    private companion object {
        fun quote(symbol: String, price: String) = QuoteDto(
            symbol = symbol,
            price = price,
            previousClose = price,
            open = price,
            dayHigh = price,
            dayLow = price,
            timestamp = "2026-11-14T10:00:00Z",
        )
    }
}
