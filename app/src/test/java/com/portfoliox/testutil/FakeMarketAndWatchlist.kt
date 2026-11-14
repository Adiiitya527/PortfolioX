package com.portfoliox.testutil

import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.WatchlistRepository
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.domain.model.WatchlistEntry
import com.portfoliox.domain.model.toMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** Test data shared by ViewModel tests. */
object TestStocks {
    val AAPL = Stock("AAPL", "Apple Inc.", "NASDAQ", "Technology")
    val AMD = Stock("AMD", "Advanced Micro Devices, Inc.", "NASDAQ", "Semiconductors")
    val MSFT = Stock("MSFT", "Microsoft Corporation", "NASDAQ", "Technology")
    val all = listOf(AAPL, AMD, MSFT)

    fun quote(symbol: String, price: String, previousClose: String = price) = Quote(
        symbol = symbol,
        price = price.toMoney(),
        previousClose = previousClose.toMoney(),
        open = previousClose.toMoney(),
        dayHigh = price.toMoney(),
        dayLow = price.toMoney(),
        timestamp = Instant.EPOCH,
    )
}

/**
 * A controllable MarketRepository. Prices are pushed through [quotes]; set [failure] to make
 * one-shot calls throw. [searchCalls] records each query so tests can check debouncing.
 */
class FakeMarketRepository(
    private val stocks: List<Stock> = TestStocks.all,
) : MarketRepository {

    val quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    var failure: Throwable? = null
    val searchCalls = mutableListOf<String>()

    /** Price history returned per range. Missing ranges return an empty list. */
    var history: Map<ChartRange, List<PricePoint>> = emptyMap()
    var historyFailure: Throwable? = null
    val historyCalls = mutableListOf<ChartRange>()

    override suspend fun searchStocks(query: String): List<Stock> {
        searchCalls += query
        failure?.let { throw it }
        return stocks.filter { query.isBlank() || it.symbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun getStock(symbol: String): Stock {
        failure?.let { throw it }
        return stocks.firstOrNull { it.symbol == symbol } ?: throw StockNotFoundException(symbol)
    }

    override suspend fun getQuote(symbol: String): Quote =
        quotes.value[symbol] ?: throw StockNotFoundException(symbol)

    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> =
        quotes.map { all -> all.filterKeys { it in symbols } }

    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> {
        historyCalls += range
        historyFailure?.let { throw it }
        return history[range].orEmpty()
    }
}

/** An in-memory WatchlistRepository that keeps the same ordering rule as Room (newest first). */
class FakeWatchlistRepository(
    private val now: () -> Instant = { Instant.parse("2026-11-14T10:00:00Z") },
) : WatchlistRepository {

    val entries = MutableStateFlow<List<WatchlistEntry>>(emptyList())

    override fun observeEntries(): Flow<List<WatchlistEntry>> =
        entries.map { list -> list.sortedByDescending { it.addedAt } }

    override fun observeIsWatched(symbol: String): Flow<Boolean> =
        entries.map { list -> list.any { it.symbol == symbol } }

    override suspend fun add(symbol: String) {
        if (entries.value.none { it.symbol == symbol }) entries.update { it + WatchlistEntry(symbol, now()) }
    }

    override suspend fun remove(symbol: String) = entries.update { list -> list.filterNot { it.symbol == symbol } }

    override suspend fun restore(entry: WatchlistEntry) {
        if (entries.value.none { it.symbol == entry.symbol }) entries.update { it + entry }
    }
}
