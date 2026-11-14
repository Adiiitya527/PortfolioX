package com.portfoliox.data.repository

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Market data for the UI layer. ViewModels depend on this interface and never on a data source,
 * so caching or a second source can be added here later without changing any ViewModel.
 *
 * Errors are reported as exceptions (for example StockNotFoundException or, with a real API,
 * IOException). ViewModels catch them and turn them into an error UI state.
 */
interface MarketRepository {
    suspend fun searchStocks(query: String): List<Stock>
    suspend fun getStock(symbol: String): Stock

    /**
     * Details for several symbols at once, in the given order, fetched in parallel.
     * Symbols the market doesn't know are left out rather than failing the whole call.
     */
    suspend fun getStocks(symbols: Collection<String>): List<Stock>
    suspend fun getQuote(symbol: String): Quote
    fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>>
    suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint>
}

/**
 * Forwards to the data source, and caches stock details (name, exchange, sector), which
 * almost never change. Prices and history aren't cached because they go stale.
 *
 * No dispatcher switching is needed: suspend functions from Room and Retrofit are "main-safe",
 * meaning they move their own blocking work off the main thread.
 */
class MarketRepositoryImpl(
    private val dataSource: MarketDataSource,
) : MarketRepository {

    // ConcurrentHashMap: several coroutines may read and write the cache at the same time.
    private val stockCache = ConcurrentHashMap<String, Stock>()

    override suspend fun searchStocks(query: String): List<Stock> =
        dataSource.searchStocks(query).onEach { stockCache[it.symbol] = it }

    override suspend fun getStock(symbol: String): Stock =
        stockCache[symbol] ?: dataSource.getStock(symbol).also { stockCache[symbol] = it }

    /**
     * `async` starts each lookup, `awaitAll` waits for all of them: 10 symbols at 300 ms each
     * take ~300 ms instead of 3 s. coroutineScope means that if one lookup fails unexpectedly,
     * the others are cancelled too (structured concurrency).
     */
    override suspend fun getStocks(symbols: Collection<String>): List<Stock> = coroutineScope {
        symbols
            .map { symbol -> async { findStock(symbol) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun findStock(symbol: String): Stock? =
        try {
            getStock(symbol)
        } catch (_: StockNotFoundException) {
            null
        }

    override suspend fun getQuote(symbol: String): Quote = dataSource.getQuote(symbol)

    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> = dataSource.observeQuotes(symbols)

    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> =
        dataSource.getPriceHistory(symbol, range)
}
