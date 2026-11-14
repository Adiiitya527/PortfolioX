package com.portfoliox.ui.stockdetail

import androidx.lifecycle.SavedStateHandle
import com.portfoliox.R
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.testutil.FakeMarketRepository
import com.portfoliox.testutil.FakePortfolioRepository
import com.portfoliox.testutil.FakeWatchlistRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.testutil.TestStocks
import com.portfoliox.testutil.assertMoney
import com.portfoliox.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository()
    private val watchlist = FakeWatchlistRepository()
    private val portfolio = FakePortfolioRepository()

    private val monthPoints = listOf("220.00", "225.00", "231.00").mapIndexed { i, close ->
        PricePoint(Instant.ofEpochSecond(i.toLong()), BigDecimal(close))
    }

    private fun TestScope.createViewModel(symbol: String = "AAPL"): StockDetailViewModel {
        val viewModel = StockDetailViewModel(symbol, market, watchlist, portfolio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.chartState.collect {} }
        return viewModel
    }

    private fun StockDetailViewModel.detail(): StockDetail =
        (uiState.value as? UiState.Success)?.data ?: error("Expected Success but was ${uiState.value}")

    @Test
    fun `shows stock info first, then the live price when it arrives`() = runTest {
        val viewModel = createViewModel()

        assertEquals(TestStocks.AAPL, viewModel.detail().stock)
        assertNull(viewModel.detail().quote)

        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "231.00", previousClose = "229.00"))

        assertMoney("231.00", viewModel.detail().quote?.price)
    }

    @Test
    fun `unknown symbol shows a not-found error`() = runTest {
        val viewModel = createViewModel(symbol = "NOPE")

        assertEquals(UiState.Error(R.string.error_stock_not_found), viewModel.uiState.value)
    }

    @Test
    fun `star toggles the watchlist`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.detail().isWatched)

        viewModel.toggleWatchlist()
        assertTrue(viewModel.detail().isWatched)

        viewModel.toggleWatchlist()
        assertFalse(viewModel.detail().isWatched)
    }

    @Test
    fun `position is valued at the live price when the user owns shares`() = runTest {
        portfolio.holdings.value = listOf(Holding("AAPL", quantity = 10, averageCost = BigDecimal("200")))
        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "231.00"))

        val viewModel = createViewModel()

        val position = viewModel.detail().position
        assertEquals(10, position?.holding?.quantity)
        assertMoney("2310.00", position?.marketValue)
        assertMoney("310.00", position?.unrealizedPnl)
    }

    @Test
    fun `no position is shown when the user owns no shares`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.detail().position)
    }

    @Test
    fun `chart loads the default range, and selecting a range loads that one`() = runTest {
        market.history = mapOf(ChartRange.MONTH to monthPoints, ChartRange.YEAR to monthPoints.take(2))
        val viewModel = createViewModel()

        assertEquals(ChartUiState(ChartRange.MONTH, UiState.Success(monthPoints)), viewModel.chartState.value)

        viewModel.selectRange(ChartRange.YEAR)

        assertEquals(ChartRange.YEAR, viewModel.chartState.value.range)
        assertEquals(UiState.Success(monthPoints.take(2)), viewModel.chartState.value.points)
        assertEquals(listOf(ChartRange.MONTH, ChartRange.YEAR), market.historyCalls)
    }

    @Test
    fun `selected range is restored from saved state after process death`() = runTest {
        market.history = mapOf(ChartRange.YEAR to monthPoints)
        val savedState = SavedStateHandle()
        val first = StockDetailViewModel("AAPL", market, watchlist, portfolio, savedState)
        first.selectRange(ChartRange.YEAR)

        // A new ViewModel built from the same saved state, as after Android recreates the process.
        val restored = StockDetailViewModel("AAPL", market, watchlist, portfolio, savedState)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { restored.chartState.collect {} }

        assertEquals(ChartRange.YEAR, restored.chartState.value.range)
    }

    @Test
    fun `chart failure doesn't affect the rest of the screen and can be retried`() = runTest {
        market.historyFailure = IOException("offline")
        market.history = mapOf(ChartRange.MONTH to monthPoints)
        val viewModel = createViewModel()

        assertEquals(UiState.Error(R.string.error_network), viewModel.chartState.value.points)
        assertTrue(viewModel.uiState.value is UiState.Success) // header still works

        market.historyFailure = null
        viewModel.retryChart()

        assertEquals(UiState.Success(monthPoints), viewModel.chartState.value.points)
    }
}
