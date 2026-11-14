package com.portfoliox.ui.history

import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/** The filter chips above the list. */
enum class HistoryFilter {
    ALL,
    BUYS,
    SELLS,
    ;

    fun matches(transaction: Transaction): Boolean = when (this) {
        ALL -> true
        BUYS -> transaction.type == TradeType.BUY
        SELLS -> transaction.type == TradeType.SELL
    }
}

/** All trades made on one calendar day, newest first. Rendered as a header plus rows. */
data class HistoryDay(
    val date: LocalDate,
    val transactions: List<Transaction>,
)

/** Everything the History screen shows. */
data class HistoryData(
    val filter: HistoryFilter,
    /** Days newest first; empty when the filter matches nothing. */
    val days: List<HistoryDay>,
    /** Profit or loss locked in by every sell, regardless of the filter. */
    val totalRealizedPnl: BigDecimal,
    val tradeCount: Int,
    /** "Today" in the user's time zone, so headers can say Today / Yesterday. */
    val today: LocalDate,
)

/**
 * Groups transactions by the calendar day they happened on in [zone], newest day first.
 * The zone matters: a trade at 23:30 UTC is "tomorrow" in India (UTC+5:30).
 *
 * `groupBy` keeps first-seen order, and the input is already newest first, so the days come out
 * newest first and each day's trades stay newest first.
 */
fun groupByDay(transactions: List<Transaction>, zone: ZoneId): List<HistoryDay> =
    transactions
        .groupBy { it.timestamp.atZone(zone).toLocalDate() }
        .map { (date, trades) -> HistoryDay(date, trades) }

/** How a day header is labelled. A sealed type keeps the wording decision in the UI. */
sealed interface RelativeDay {
    data object Today : RelativeDay
    data object Yesterday : RelativeDay
    data class On(val date: LocalDate) : RelativeDay
}

fun relativeDay(date: LocalDate, today: LocalDate): RelativeDay = when (date) {
    today -> RelativeDay.Today
    today.minusDays(1) -> RelativeDay.Yesterday
    else -> RelativeDay.On(date)
}
