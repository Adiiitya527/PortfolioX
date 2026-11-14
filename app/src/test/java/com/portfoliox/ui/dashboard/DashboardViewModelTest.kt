package com.portfoliox.ui.dashboard

import com.portfoliox.R
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.FakeMarketRepository
import com.portfoliox.testutil.FakePortfolioRepository
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val portfolio = FakePortfolioRepository()
    private val market = FakeMarketRepository()

    /**
     * stateIn(WhileSubscribed) only runs the upstream while someone collects, the way the
     * screen does. backgroundScope is cancelled automatically when the test ends.
     */
    private fun TestScope.createViewModel(): DashboardViewModel {
        val viewModel = DashboardViewModel(portfolio, market)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun DashboardViewModel.data(): DashboardData =
        (uiState.value as? UiState.Success)?.data ?: error("Expected Success but was ${uiState.value}")

    private fun holding(symbol: String, quantity: Int, averageCost: String) =
        Holding(symbol, quantity, BigDecimal(averageCost))

    @Test
    fun `state is Loading before anything is collected`() {
        assertEquals(UiState.Loading, DashboardViewModel(portfolio, market).uiState.value)
    }

    @Test
    fun `new account shows the starting cash and no holdings`() = runTest {
        val data = createViewModel().data()

        assertMoney("100000.00", data.summary.totalValue)
        assertMoney("0", data.summary.invested)
        assertTrue(data.holdings.isEmpty())
    }

    @Test
    fun `summary and rows use live prices, largest position first`() = runTest {
        portfolio.cash.value = "1000.00".toMoney()
        portfolio.holdings.value = listOf(
            holding("AAPL", 10, "150"), // cost 1500, value 1600 → +100
            holding("MSFT", 2, "400"), //  cost 800,  value 760  → −40
        )
        market.quotes.value = mapOf(
            "AAPL" to TestStocks.quote("AAPL", "160.00", previousClose = "155.00"),
            "MSFT" to TestStocks.quote("MSFT", "380.00", previousClose = "390.00"),
        )

        val data = createViewModel().data()

        assertMoney("3360.00", data.summary.totalValue)
        assertMoney("2300.00", data.summary.invested)
        assertMoney("60.00", data.summary.unrealizedPnl)
        assertMoney("30.00", data.summary.dayChange)
        assertEquals(listOf("AAPL", "MSFT"), data.holdings.map { it.symbol })
        assertEquals("Apple Inc.", data.holdings.first().name)
        assertMoney("67.80", data.holdings.first().allocationPercent) // 1600 / 2360
    }

    @Test
    fun `price ticks update the dashboard`() = runTest {
        portfolio.holdings.value = listOf(holding("AAPL", 10, "150"))
        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "150.00"))
        val viewModel = createViewModel()
        assertMoney("0.00", viewModel.data().summary.unrealizedPnl)

        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "155.00"))

        assertMoney("50.00", viewModel.data().summary.unrealizedPnl)
    }

    @Test
    fun `a trade that changes holdings and cash updates the dashboard`() = runTest {
        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "100.00"), "MSFT" to TestStocks.quote("MSFT", "200.00"))
        portfolio.holdings.value = listOf(holding("AAPL", 1, "100"))
        val viewModel = createViewModel()

        // Simulates what PortfolioRepository does after buying 2 MSFT at $200.
        portfolio.holdings.value = listOf(holding("AAPL", 1, "100"), holding("MSFT", 2, "200"))
        portfolio.cash.value = "99600.00".toMoney()

        val data = viewModel.data()
        assertEquals(listOf("MSFT", "AAPL"), data.holdings.map { it.symbol })
        assertMoney("100100.00", data.summary.totalValue) // 99 600 cash + 400 + 100
    }

    @Test
    fun `holdings without a quote yet are valued at cost`() = runTest {
        portfolio.holdings.value = listOf(holding("AAPL", 10, "150"))

        val row = createViewModel().data().holdings.single()

        assertMoney("1500.00", row.valuation.marketValue)
        assertMoney("0", row.valuation.unrealizedPnl)
    }

    @Test
    fun `a stock the market no longer lists still shows, named by its symbol`() = runTest {
        portfolio.holdings.value = listOf(holding("GONE", 5, "10"))

        val row = createViewModel().data().holdings.single()

        assertEquals("GONE", row.name)
    }

    @Test
    fun `repository failure shows an error, and retry recovers`() = runTest {
        portfolio.failure = IOException("disk unavailable")
        val viewModel = createViewModel()
        assertEquals(UiState.Error(R.string.error_network), viewModel.uiState.value)

        portfolio.failure = null
        viewModel.retry()

        assertMoney("100000.00", viewModel.data().summary.totalValue)
    }
}
