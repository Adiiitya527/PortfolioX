package com.portfoliox.data.market.remote

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.data.market.remote.dto.QuoteDto
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection

/**
 * A [MarketDataSource] backed by the REST API. It implements the same interface as
 * SampleMarketDataSource, so no repository, ViewModel or screen changes when it's switched on
 * in AppContainer.
 *
 * Live prices use **polling**: request /quotes every [pollIntervalMillis]. It's simple, works
 * through any proxy, and a 3-second delay is fine for a demo. A production trading app would
 * push prices over a WebSocket or server-sent events instead.
 */
class RemoteMarketDataSource(
    private val api: MarketApi,
    private val pollIntervalMillis: Long = 3_000,
    private val maxBackoffMillis: Long = 30_000,
) : MarketDataSource {

    override suspend fun getStocks(): List<Stock> = api.searchStocks("").map { it.toDomain() }

    override suspend fun searchStocks(query: String): List<Stock> =
        api.searchStocks(query.trim()).map { it.toDomain() }

    override suspend fun getStock(symbol: String): Stock =
        notFoundAsDomainError(symbol) { api.getStock(symbol).toDomain() }

    override suspend fun getQuote(symbol: String): Quote =
        api.getQuotes(symbol).firstOrNull()?.toDomain() ?: throw StockNotFoundException(symbol)

    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> =
        notFoundAsDomainError(symbol) { api.getHistory(symbol, range.label).points.map { it.toDomain() } }

    /**
     * Polls for prices while collected. A failed poll (no connection, server error) doesn't end
     * the stream: it waits with exponential backoff (1 s, 2 s, 4 s … up to 30 s) and tries again,
     * so screens keep their last prices and recover by themselves when the server is back.
     *
     * `emit` is deliberately outside the try/catch: exceptions thrown by the collector must not be
     * caught here (Flow's "exception transparency" rule).
     */
    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> = flow {
        if (symbols.isEmpty()) {
            emit(emptyMap())
            return@flow
        }
        val query = symbols.sorted().joinToString(",")
        var consecutiveFailures = 0

        while (true) {
            val quotes: List<QuoteDto>? = try {
                api.getQuotes(query)
            } catch (e: Exception) {
                if (!e.isTransient()) throw e
                null
            }

            if (quotes != null) {
                consecutiveFailures = 0
                emit(quotes.associate { it.symbol to it.toDomain() })
                delay(pollIntervalMillis)
            } else {
                consecutiveFailures++
                delay(backoffMillis(consecutiveFailures))
            }
        }
    }

    internal fun backoffMillis(failures: Int): Long {
        val exponential = 1_000L shl (failures - 1).coerceIn(0, 15) // 1 s, 2 s, 4 s, …
        return exponential.coerceAtMost(maxBackoffMillis)
    }

    /** Network problems and 5xx server errors may succeed on retry; 4xx or bad data won't. */
    private fun Exception.isTransient(): Boolean =
        this is IOException || (this is HttpException && code() >= 500)

    /** Translates an HTTP 404 into the same exception the sample source throws. */
    private inline fun <T> notFoundAsDomainError(symbol: String, block: () -> T): T =
        try {
            block()
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) throw StockNotFoundException(symbol)
            throw e
        }
}
