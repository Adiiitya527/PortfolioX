package com.portfoliox.data.repository

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.sample.SampleMarketDataSource
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketRepositoryImplTest {

    /**
     * Kotlin class delegation: `MarketDataSource by inner` forwards every method to `inner`,
     * and we override only the one we want to count. No mocking library needed.
     */
    private class CountingDataSource(
        private val inner: MarketDataSource,
    ) : MarketDataSource by inner {
        var getStockCalls = 0

        override suspend fun getStock(symbol: String): Stock {
            getStockCalls++
            return inner.getStock(symbol)
        }
    }

    private val source = CountingDataSource(SampleMarketDataSource(latencyMillis = 0))
    private val repository = MarketRepositoryImpl(source)

    @Test
    fun `stock details are fetched once and then served from the cache`() = runTest {
        val first = repository.getStock("AAPL")
        val second = repository.getStock("AAPL")

        assertEquals(first, second)
        assertEquals(1, source.getStockCalls)
    }

    @Test
    fun `search results fill the cache too`() = runTest {
        repository.searchStocks("IBKR")

        repository.getStock("IBKR")

        assertEquals(0, source.getStockCalls)
    }
}
