package com.portfoliox.ui.search

import com.portfoliox.R
import com.portfoliox.testutil.FakeMarketRepository
import com.portfoliox.testutil.FakeWatchlistRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.testutil.TestStocks
import com.portfoliox.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Debounce is time-based, so these tests control virtual time explicitly:
 * advanceTimeBy(ms) moves the clock forward and runs whatever became due.
 * runTest shares its scheduler with the Main dispatcher set by MainDispatcherRule.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository()
    private val watchlist = FakeWatchlistRepository()

    private fun TestScope.createViewModel(): SearchViewModel {
        val viewModel = SearchViewModel(market, watchlist)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    /** Lets the debounce window pass so the pending search runs. */
    private fun TestScope.passDebounce() {
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        runCurrent()
    }

    private fun SearchViewModel.symbols(): List<String> =
        (uiState.value as? UiState.Success)?.data?.map { it.stock.symbol } ?: error("Expected Success but was ${uiState.value}")

    @Test
    fun `blank query lists every stock once the debounce passes`() = runTest {
        val viewModel = createViewModel()
        assertEquals(UiState.Loading, viewModel.uiState.value)

        passDebounce()

        assertEquals(listOf("AAPL", "AMD", "MSFT"), viewModel.symbols())
    }

    @Test
    fun `fast typing runs a single search for the final text`() = runTest {
        val viewModel = createViewModel()
        passDebounce()
        market.searchCalls.clear()

        viewModel.onQueryChange("m")
        advanceTimeBy(100)
        viewModel.onQueryChange("ms")
        advanceTimeBy(100)
        viewModel.onQueryChange("msft")
        passDebounce()

        assertEquals(listOf("msft"), market.searchCalls)
        assertEquals(listOf("MSFT"), viewModel.symbols())
    }

    @Test
    fun `query with no matches shows Empty`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("zzzz")
        passDebounce()

        assertEquals(UiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `star toggles watchlist membership and the result updates`() = runTest {
        val viewModel = createViewModel()
        passDebounce()
        val aapl = (viewModel.uiState.value as UiState.Success).data.first { it.stock == TestStocks.AAPL }
        assertFalse(aapl.isWatched)

        viewModel.toggleWatchlist(aapl)

        val updated = (viewModel.uiState.value as UiState.Success).data.first { it.stock == TestStocks.AAPL }
        assertTrue(updated.isWatched)
        assertEquals(listOf("AAPL"), watchlist.entries.value.map { it.symbol })

        viewModel.toggleWatchlist(updated)
        assertTrue(watchlist.entries.value.isEmpty())
    }

    @Test
    fun `failure shows an error, and retry searches again`() = runTest {
        market.failure = IOException("offline")
        val viewModel = createViewModel()
        passDebounce()
        assertEquals(UiState.Error(R.string.error_network), viewModel.uiState.value)

        market.failure = null
        viewModel.retry()
        runCurrent()

        assertEquals(listOf("AAPL", "AMD", "MSFT"), viewModel.symbols())
    }
}
