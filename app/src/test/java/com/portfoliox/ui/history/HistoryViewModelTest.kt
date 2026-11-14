package com.portfoliox.ui.history

import com.portfoliox.R
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import com.portfoliox.testutil.FakePortfolioRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.testutil.assertMoney
import com.portfoliox.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val portfolio = FakePortfolioRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC)

    // Newest first, as the repository returns them.
    private val trades = listOf(
        Transaction(4, "AAPL", TradeType.SELL, 5, BigDecimal("160.00"), BigDecimal("800.00"), BigDecimal("50.00"), Instant.parse("2026-11-14T15:00:00Z")),
        Transaction(3, "MSFT", TradeType.SELL, 1, BigDecimal("390.00"), BigDecimal("390.00"), BigDecimal("-10.00"), Instant.parse("2026-11-14T11:00:00Z")),
        Transaction(2, "MSFT", TradeType.BUY, 1, BigDecimal("400.00"), BigDecimal("400.00"), null, Instant.parse("2026-11-13T10:00:00Z")),
        Transaction(1, "AAPL", TradeType.BUY, 10, BigDecimal("150.00"), BigDecimal("1500.00"), null, Instant.parse("2026-11-12T10:00:00Z")),
    )

    private fun TestScope.createViewModel(): HistoryViewModel {
        val viewModel = HistoryViewModel(portfolio, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun HistoryViewModel.data(): HistoryData =
        (uiState.value as? UiState.Success)?.data ?: error("Expected Success but was ${uiState.value}")

    @Test
    fun `no trades shows the Empty state`() = runTest {
        assertEquals(UiState.Empty, createViewModel().uiState.value)
    }

    @Test
    fun `trades are grouped into days with totals`() = runTest {
        portfolio.transactions.value = trades

        val data = createViewModel().data()

        assertEquals(listOf("2026-11-14", "2026-11-13", "2026-11-12"), data.days.map { it.date.toString() })
        assertEquals(listOf(4L, 3L), data.days.first().transactions.map { it.id })
        assertMoney("40.00", data.totalRealizedPnl) // +50 − 10
        assertEquals(4, data.tradeCount)
        assertEquals(LocalDate.of(2026, 11, 14), data.today)
    }

    @Test
    fun `filters narrow the list but not the realized total`() = runTest {
        portfolio.transactions.value = trades
        val viewModel = createViewModel()

        viewModel.selectFilter(HistoryFilter.BUYS)
        assertEquals(listOf(2L, 1L), viewModel.data().days.flatMap { day -> day.transactions.map { it.id } })
        assertMoney("40.00", viewModel.data().totalRealizedPnl)

        viewModel.selectFilter(HistoryFilter.SELLS)
        assertEquals(listOf(4L, 3L), viewModel.data().days.flatMap { day -> day.transactions.map { it.id } })
    }

    @Test
    fun `a filter with no matches keeps the screen and shows an empty list`() = runTest {
        portfolio.transactions.value = trades.filter { it.type == TradeType.BUY }
        val viewModel = createViewModel()

        viewModel.selectFilter(HistoryFilter.SELLS)

        assertEquals(HistoryFilter.SELLS, viewModel.data().filter)
        assertTrue(viewModel.data().days.isEmpty())
    }

    @Test
    fun `a new trade appears at the top`() = runTest {
        portfolio.transactions.value = trades
        val viewModel = createViewModel()

        val newTrade = trades.first().copy(id = 5, timestamp = Instant.parse("2026-11-14T17:00:00Z"))
        portfolio.transactions.value = listOf(newTrade) + trades

        assertEquals(5L, viewModel.data().days.first().transactions.first().id)
    }

    @Test
    fun `failure shows an error, and retry recovers`() = runTest {
        portfolio.failure = IOException("disk unavailable")
        val viewModel = createViewModel()
        assertEquals(UiState.Error(R.string.error_network), viewModel.uiState.value)

        portfolio.failure = null
        portfolio.transactions.value = trades
        viewModel.retry()

        assertEquals(4, viewModel.data().tradeCount)
    }
}
