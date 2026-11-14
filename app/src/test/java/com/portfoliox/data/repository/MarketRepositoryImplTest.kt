package com.portfoliox.data.repository

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.sample.SampleMarketDataSource
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MarketRepositoryImplTest {

    /**
     * Kotlin class delegation: `MarketDataSource by inner` forwards every method to `inner`,
     * and we override only the ones we want to count. No mocking library needed.
     */
    private class CountingDataSource(
        private val inner: MarketDataSource,
    ) : MarketDataSource by inner {
        var getStockCalls = 0
        var quoteStreamsStarted = 0
        var failNextQuoteStream = false

        override suspend fun getStock(symbol: String): Stock {
            getStockCalls++
            return inner.getStock(symbol)
        }

        override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> = flow {
            quoteStreamsStarted++
            if (failNextQuoteStream) {
                failNextQuoteStream = false
                throw IOException("server unreachable")
            }
            emitAll(inner.observeQuotes(symbols))
        }
    }

    private val source = CountingDataSource(SampleMarketDataSource(latencyMillis = 0))

    /** Shared streams run in the test's backgroundScope, which is cancelled when the test ends. */
    private fun TestScope.repository() = MarketRepositoryImpl(source, sharingScope = backgroundScope)

    @Test
    fun `stock details are fetched once and then served from the cache`() = runTest {
        val repository = repository()

        val first = repository.getStock("AAPL")
        val second = repository.getStock("AAPL")

        assertEquals(first, second)
        assertEquals(1, source.getStockCalls)
    }

    @Test
    fun `getStocks keeps the requested order and skips unknown symbols`() = runTest {
        val stocks = repository().getStocks(listOf("MSFT", "NOPE", "AAPL"))

        assertEquals(listOf("MSFT", "AAPL"), stocks.map { it.symbol })
    }

    @Test
    fun `search results fill the cache too`() = runTest {
        val repository = repository()
        repository.searchStocks("IBKR")

        repository.getStock("IBKR")

        assertEquals(0, source.getStockCalls)
    }

    @Test
    fun `two screens watching the same symbols share one upstream stream`() = runTest {
        val repository = repository()

        val detailScreen = async { repository.observeQuotes(setOf("KO")).take(2).toList() }
        val tradeSheet = async { repository.observeQuotes(setOf("KO")).take(2).toList() }

        assertEquals(2, detailScreen.await().size)
        assertEquals(2, tradeSheet.await().size)
        assertEquals(1, source.quoteStreamsStarted)
    }

    @Test
    fun `different symbol sets get their own streams`() = runTest {
        val repository = repository()

        repository.observeQuotes(setOf("KO")).first()
        repository.observeQuotes(setOf("KO", "AAPL")).first()

        assertEquals(2, source.quoteStreamsStarted)
    }

    @Test
    fun `a failed stream reaches the subscriber, and the next subscription starts fresh`() = runTest {
        val repository = repository()
        source.failNextQuoteStream = true

        val failure = runCatching { repository.observeQuotes(setOf("KO")).first() }
        assertTrue(failure.exceptionOrNull() is IOException)

        val retry = repository.observeQuotes(setOf("KO")).first()
        assertTrue("KO" in retry)
        assertEquals(2, source.quoteStreamsStarted)
    }
}
