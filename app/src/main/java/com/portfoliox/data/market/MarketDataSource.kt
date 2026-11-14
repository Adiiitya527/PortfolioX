package com.portfoliox.data.market

import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import kotlinx.coroutines.flow.Flow

/**
 * Where market data comes from. The rest of the app depends only on this interface,
 * so the sample implementation can be replaced by a REST API (Phase 9) without touching any screen.
 *
 * Two kinds of async API:
 * - `suspend fun` returns one value, later. Web analogy: an async function returning a Promise.
 * - `Flow` emits many values over time. Web analogy: an RxJS Observable or a WebSocket stream.
 */
interface MarketDataSource {

    /** Full list of tradable stocks. */
    suspend fun getStocks(): List<Stock>

    /** Stocks whose symbol or name matches [query], best matches first. A blank query returns all stocks. */
    suspend fun searchStocks(query: String): List<Stock>

    /** Static details for one stock. @throws StockNotFoundException for an unknown symbol. */
    suspend fun getStock(symbol: String): Stock

    /** Latest price for one stock. @throws StockNotFoundException for an unknown symbol. */
    suspend fun getQuote(symbol: String): Quote

    /**
     * Live prices for [symbols], emitted again every time prices update.
     * Unknown symbols are left out of the map instead of failing the whole stream.
     */
    fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>>

    /** Historical closing prices, oldest first. @throws StockNotFoundException for an unknown symbol. */
    suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint>
}

/** Thrown when a symbol isn't known to the market data source. */
class StockNotFoundException(val symbol: String) : Exception("Unknown stock symbol: $symbol")
