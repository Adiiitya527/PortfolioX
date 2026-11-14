package com.portfoliox.data.repository

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.flow.Flow

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
    suspend fun getQuote(symbol: String): Quote
    fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>>
    suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint>
}

/**
 * Today this forwards straight to the data source. It's the place where caching
 * (for example keeping price history in memory) would go when it's needed.
 *
 * No dispatcher switching is needed: suspend functions from Room and Retrofit are "main-safe",
 * meaning they move their own blocking work off the main thread.
 */
class MarketRepositoryImpl(
    private val dataSource: MarketDataSource,
) : MarketRepository {

    override suspend fun searchStocks(query: String): List<Stock> = dataSource.searchStocks(query)

    override suspend fun getStock(symbol: String): Stock = dataSource.getStock(symbol)

    override suspend fun getQuote(symbol: String): Quote = dataSource.getQuote(symbol)

    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> = dataSource.observeQuotes(symbols)

    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> =
        dataSource.getPriceHistory(symbol, range)
}
