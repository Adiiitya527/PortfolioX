package com.portfoliox.data.market.sample

import com.portfoliox.domain.model.Stock
import com.portfoliox.domain.model.toMoney
import java.math.BigDecimal

/**
 * A sample stock: the [Stock] plus the numbers the simulator needs.
 *
 * @property referencePrice yesterday's closing price; intraday prices move around it.
 * @property dailyVolatility typical daily move as a fraction (0.02 = about 2% per day).
 */
data class SampleStock(
    val stock: Stock,
    val referencePrice: BigDecimal,
    val dailyVolatility: Double,
)

/**
 * Sample market data. Prices are illustrative, not live quotes.
 * `internal` limits visibility to this module.
 */
internal object SampleStocks {

    // A private helper keeps the list below readable.
    private fun sample(
        symbol: String,
        name: String,
        exchange: String,
        sector: String,
        price: String,
        volatility: Double,
    ) = SampleStock(
        stock = Stock(symbol = symbol, name = name, exchange = exchange, sector = sector),
        referencePrice = price.toMoney(),
        dailyVolatility = volatility,
    )

    val all: List<SampleStock> = listOf(
        sample("AAPL", "Apple Inc.", "NASDAQ", "Technology", "231.45", 0.015),
        sample("MSFT", "Microsoft Corporation", "NASDAQ", "Technology", "428.10", 0.014),
        sample("NVDA", "NVIDIA Corporation", "NASDAQ", "Semiconductors", "142.80", 0.030),
        sample("GOOGL", "Alphabet Inc. Class A", "NASDAQ", "Communication Services", "176.25", 0.017),
        sample("AMZN", "Amazon.com, Inc.", "NASDAQ", "Consumer Discretionary", "198.60", 0.018),
        sample("META", "Meta Platforms, Inc.", "NASDAQ", "Communication Services", "585.30", 0.020),
        sample("TSLA", "Tesla, Inc.", "NASDAQ", "Consumer Discretionary", "251.75", 0.035),
        sample("NFLX", "Netflix, Inc.", "NASDAQ", "Communication Services", "702.40", 0.022),
        sample("AMD", "Advanced Micro Devices, Inc.", "NASDAQ", "Semiconductors", "158.90", 0.028),
        sample("IBKR", "Interactive Brokers Group, Inc.", "NASDAQ", "Financials", "187.55", 0.019),
        sample("JPM", "JPMorgan Chase & Co.", "NYSE", "Financials", "222.35", 0.012),
        sample("GS", "The Goldman Sachs Group, Inc.", "NYSE", "Financials", "538.20", 0.016),
        sample("V", "Visa Inc.", "NYSE", "Financials", "289.70", 0.011),
        sample("KO", "The Coca-Cola Company", "NYSE", "Consumer Staples", "68.15", 0.008),
        sample("DIS", "The Walt Disney Company", "NYSE", "Communication Services", "97.40", 0.016),
    )
}
