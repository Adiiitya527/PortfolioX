package com.portfoliox.data.market.remote

import com.portfoliox.data.market.remote.dto.HistoryDto
import com.portfoliox.data.market.remote.dto.QuoteDto
import com.portfoliox.data.market.remote.dto.StockDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The REST API, declared as an interface. Retrofit generates the implementation: each
 * annotated function becomes an HTTP request, and the JSON response is parsed into the return
 * type. `suspend` functions run off the main thread and throw on failure:
 * - IOException: no connection, timeout
 * - HttpException: the server answered with a non-2xx status (e.g. 404)
 *
 * Web analogy: a typed client like `const api = { getStock: (s) => axios.get(`/stocks/${s}`) }`.
 */
interface MarketApi {

    @GET("stocks")
    suspend fun searchStocks(@Query("query") query: String): List<StockDto>

    @GET("stocks/{symbol}")
    suspend fun getStock(@Path("symbol") symbol: String): StockDto

    /** @param symbols comma-separated, e.g. "AAPL,MSFT" */
    @GET("quotes")
    suspend fun getQuotes(@Query("symbols") symbols: String): List<QuoteDto>

    /** @param range "1W", "1M", "3M" or "1Y" */
    @GET("stocks/{symbol}/history")
    suspend fun getHistory(@Path("symbol") symbol: String, @Query("range") range: String): HistoryDto
}
