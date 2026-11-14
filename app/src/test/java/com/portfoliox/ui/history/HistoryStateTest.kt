package com.portfoliox.ui.history

import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HistoryStateTest {

    private fun trade(id: Long, at: String, type: TradeType = TradeType.BUY) = Transaction(
        id = id, symbol = "AAPL", type = type, quantity = 1, price = BigDecimal.TEN, total = BigDecimal.TEN,
        realizedPnl = if (type == TradeType.SELL) BigDecimal.ONE else null, timestamp = Instant.parse(at),
    )

    @Test
    fun `trades are grouped by day, newest day first, keeping order within a day`() {
        val trades = listOf(
            trade(3, "2026-11-14T15:00:00Z"),
            trade(2, "2026-11-14T09:00:00Z"),
            trade(1, "2026-11-13T12:00:00Z"),
        )

        val days = groupByDay(trades, ZoneOffset.UTC)

        assertEquals(listOf(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 13)), days.map { it.date })
        assertEquals(listOf(3L, 2L), days.first().transactions.map { it.id })
    }

    @Test
    fun `the day depends on the user's time zone`() {
        // 20:00 UTC on 13 Nov is 01:30 on 14 Nov in India.
        val trades = listOf(trade(1, "2026-11-13T20:00:00Z"))

        assertEquals(LocalDate.of(2026, 11, 13), groupByDay(trades, ZoneOffset.UTC).single().date)
        assertEquals(LocalDate.of(2026, 11, 14), groupByDay(trades, ZoneId.of("Asia/Kolkata")).single().date)
    }

    @Test
    fun `headers say Today, Yesterday, or the date`() {
        val today = LocalDate.of(2026, 11, 14)

        assertEquals(RelativeDay.Today, relativeDay(today, today))
        assertEquals(RelativeDay.Yesterday, relativeDay(today.minusDays(1), today))
        assertEquals(RelativeDay.On(today.minusDays(2)), relativeDay(today.minusDays(2), today))
    }

    @Test
    fun `filters match by trade type`() {
        val buy = trade(1, "2026-11-14T10:00:00Z", TradeType.BUY)
        val sell = trade(2, "2026-11-14T11:00:00Z", TradeType.SELL)

        assertTrue(HistoryFilter.ALL.matches(buy) && HistoryFilter.ALL.matches(sell))
        assertTrue(HistoryFilter.BUYS.matches(buy))
        assertFalse(HistoryFilter.BUYS.matches(sell))
        assertTrue(HistoryFilter.SELLS.matches(sell))
    }
}
