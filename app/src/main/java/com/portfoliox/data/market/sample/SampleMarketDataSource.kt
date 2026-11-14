package com.portfoliox.data.market.sample

import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.domain.model.toMoney
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A [MarketDataSource] that simulates a market from sample data, with no network.
 *
 * Prices are a pure function of (symbol, time). The same moment always gives the same price,
 * whoever asks, so there's no shared mutable state, and tests are repeatable with a fixed [clock].
 *
 * The simulation uses Double maths for speed. That's fine here because these are generated
 * numbers, not accounting. Values become BigDecimal (rounded to cents) before leaving this class.
 *
 * Constructor parameters with default values make the class easy to configure in tests:
 * `SampleMarketDataSource(clock = fixedClock, latencyMillis = 0)`.
 *
 * @param clock source of "now". Injected so tests can freeze time.
 * @param latencyMillis artificial delay on one-shot calls, so loading states are visible in the UI.
 * @param tickIntervalMillis how often [observeQuotes] emits new prices.
 */
class SampleMarketDataSource(
    private val clock: Clock = Clock.systemUTC(),
    private val latencyMillis: Long = 300,
    private val tickIntervalMillis: Long = 3_000,
    stocks: List<SampleStock> = SampleStocks.all,
) : MarketDataSource {

    // A map gives O(1) lookups by symbol instead of scanning the list each time.
    private val stocksBySymbol: Map<String, SampleStock> = stocks.associateBy { it.stock.symbol }

    override suspend fun getStocks(): List<Stock> {
        simulateLatency()
        return allStocksSorted()
    }

    override suspend fun searchStocks(query: String): List<Stock> {
        simulateLatency()
        val term = query.trim()
        if (term.isEmpty()) return allStocksSorted()

        return stocksBySymbol.values
            .map { it.stock }
            .filter { it.symbol.contains(term, ignoreCase = true) || it.name.contains(term, ignoreCase = true) }
            // Ranking: symbols starting with the query first ("A" -> AAPL before NVDA), then alphabetical.
            // `false` sorts before `true`, hence the negation.
            .sortedWith(compareBy<Stock>({ !it.symbol.startsWith(term, ignoreCase = true) }, { it.symbol }))
    }

    override suspend fun getStock(symbol: String): Stock {
        simulateLatency()
        return requireSample(symbol).stock
    }

    override suspend fun getQuote(symbol: String): Quote {
        simulateLatency()
        return quoteAt(requireSample(symbol), clock.instant())
    }

    /**
     * `flow { }` builds a cold Flow: the block runs separately for each collector and only while
     * it is being collected. When the screen stops collecting, the coroutine is cancelled at
     * `delay`, so the infinite loop stops by itself.
     */
    override fun observeQuotes(symbols: Set<String>): Flow<Map<String, Quote>> = flow {
        val tracked = symbols.mapNotNull { stocksBySymbol[it.uppercase()] }
        while (true) {
            val now = clock.instant()
            emit(tracked.associate { it.stock.symbol to quoteAt(it, now) })
            delay(tickIntervalMillis)
        }
    }

    /**
     * Generates a random-walk price history that ends exactly at today's price, so the chart
     * and the quote always agree. The random generator is seeded with the symbol and range,
     * so the same stock shows the same chart every time.
     */
    override suspend fun getPriceHistory(symbol: String, range: ChartRange): List<PricePoint> {
        simulateLatency()
        val sample = requireSample(symbol)
        val now = clock.instant()
        val random = Random(seedFor(symbol, range.ordinal.toLong()))

        val stepDays = range.step.toMinutes() / MINUTES_PER_DAY
        val stepVolatility = sample.dailyVolatility * sqrt(stepDays)
        val stepDrift = ANNUAL_DRIFT * stepDays / DAYS_PER_YEAR

        // Walk backwards from the current price: each earlier price = later price / (1 + return).
        val prices = DoubleArray(range.pointCount)
        prices[prices.lastIndex] = quoteAt(sample, now).price.toDouble()
        for (i in prices.lastIndex - 1 downTo 0) {
            val stepReturn = stepDrift + stepVolatility * random.nextGaussian()
            prices[i] = (prices[i + 1] / (1 + stepReturn)).coerceAtLeast(MIN_PRICE)
        }

        return prices.mapIndexed { index, price ->
            val stepsBack = prices.lastIndex - index
            PricePoint(
                timestamp = now.minus(range.step.multipliedBy(stepsBack.toLong())),
                close = price.toMoneyValue(),
            )
        }
    }

    // ---- Simulation helpers ------------------------------------------------------------------

    /**
     * Intraday price = yesterday's close × (1 + slow wave + fast wave + small noise).
     * The waves give a believable shape; the noise changes every tick so prices visibly move.
     */
    private fun quoteAt(sample: SampleStock, now: Instant): Quote {
        val reference = sample.referencePrice.toDouble()
        val volatility = sample.dailyVolatility
        val phase = (sample.stock.symbol.hashCode() % 360) * PI / 180
        val seconds = now.epochSecond.toDouble()

        val slowWave = 0.6 * sin(2 * PI * seconds / SLOW_WAVE_SECONDS + phase)
        val fastWave = 0.3 * sin(2 * PI * seconds / FAST_WAVE_SECONDS + 2 * phase)
        val tick = now.toEpochMilli() / tickIntervalMillis.coerceAtLeast(1)
        val noise = 0.1 * (Random(seedFor(sample.stock.symbol, tick)).nextDouble() * 2 - 1)
        val price = reference * (1 + volatility * (slowWave + fastWave + noise))

        val open = reference * (1 + volatility * 0.3 * sin(phase))
        val high = maxOf(open, price) * (1 + volatility * 0.25)
        val low = minOf(open, price) * (1 - volatility * 0.25)

        return Quote(
            symbol = sample.stock.symbol,
            price = price.toMoneyValue(),
            previousClose = sample.referencePrice,
            open = open.toMoneyValue(),
            dayHigh = high.toMoneyValue(),
            dayLow = low.toMoneyValue(),
            timestamp = now,
        )
    }

    private fun requireSample(symbol: String): SampleStock =
        stocksBySymbol[symbol.uppercase()] ?: throw StockNotFoundException(symbol)

    private fun allStocksSorted(): List<Stock> = stocksBySymbol.values.map { it.stock }.sortedBy { it.symbol }

    private suspend fun simulateLatency() {
        if (latencyMillis > 0) delay(latencyMillis)
    }

    private fun seedFor(symbol: String, salt: Long): Long = symbol.hashCode() * 31L + salt

    private fun Double.toMoneyValue(): BigDecimal = BigDecimal.valueOf(this).toMoney()

    // Constants shared by all instances. `const val` values are inlined at compile time.
    private companion object {
        const val SLOW_WAVE_SECONDS = 6 * 60 * 60.0
        const val FAST_WAVE_SECONDS = 47 * 60.0
        const val ANNUAL_DRIFT = 0.08
        const val DAYS_PER_YEAR = 365.0
        const val MINUTES_PER_DAY = 24 * 60.0
        const val MIN_PRICE = 1.0
    }
}
