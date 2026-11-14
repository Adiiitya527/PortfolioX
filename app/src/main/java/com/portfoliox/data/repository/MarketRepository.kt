package com.portfoliox.data.repository

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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
 * Forwards to the data source, with two optimisations:
 * - stock details (name, exchange, sector) are cached, because they almost never change;
 * - live quote streams are **shared**: screens watching the same symbols use one polling loop.
 *
 * No dispatcher switching is needed: suspend functions from Room and Retrofit are "main-safe",
 * meaning they move their own blocking work off the main thread.
 *
 * @param sharingScope lives as long as the app; shared quote streams run in it.
 */
class MarketRepositoryImpl(
    private val dataSource: MarketDataSource,
    private val sharingScope: CoroutineScope,
) : MarketRepository {

    // ConcurrentHashMap: several coroutines may read and write the cache at the same time.
    private val stockCache = ConcurrentHashMap<String, Stock>()

    /** One shared stream per distinct set of symbols. */
    private val quoteStreams = ConcurrentHashMap<Set<String>, SharedFlow<Result<Map<String, Quote>>>>()

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

    /**
     * Without sharing, the stock detail screen and the trade sheet would each poll the server
     * for the same symbol. With it, the first subscriber starts one upstream collection and every
     * later subscriber joins it.
     *
     * - shareIn turns the cold Flow into a hot SharedFlow running in [sharingScope].
     * - WhileSubscribed(5 s): polling stops 5 s after the last screen stops listening.
     * - replay = 1: a new subscriber immediately gets the latest prices.
     *
     * Errors: a SharedFlow never completes, so an upstream exception would be lost (or crash the
     * scope). Each update is wrapped in Result instead; subscribers rethrow failures with
     * getOrThrow(), so screens still show their error state, and the failed stream is removed
     * from the cache so a retry starts a fresh one.
     */
    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> {
        val key = symbols.toSet()
        val shared = quoteStreams.computeIfAbsent(key) {
            dataSource.observeQuotes(key)
                .map { Result.success(it) }
                .catch { error ->
                    quoteStreams.remove(key)
                    emit(Result.failure(error))
                }
                .shareIn(sharingScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 1)
        }
        return shared.map { it.getOrThrow() }
    }

    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> =
        dataSource.getPriceHistory(symbol, range)
}
