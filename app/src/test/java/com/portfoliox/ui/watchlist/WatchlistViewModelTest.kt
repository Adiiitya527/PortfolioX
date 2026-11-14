package com.portfoliox.ui.watchlist

import com.portfoliox.R
import com.portfoliox.domain.model.WatchlistEntry
import com.portfoliox.testutil.FakeMarketRepository
import com.portfoliox.testutil.FakeWatchlistRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.testutil.TestStocks
import com.portfoliox.testutil.assertMoney
import com.portfoliox.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository()
    private val watchlist = FakeWatchlistRepository()

    private fun TestScope.createViewModel(): WatchlistViewModel {
        val viewModel = WatchlistViewModel(watchlist, market)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    /** Smart-casts to Success or fails the test with the actual state. */
    private fun WatchlistViewModel.items(): List<WatchlistItem> =
        (uiState.value as? UiState.Success)?.data ?: error("Expected Success but was ${uiState.value}")

    private fun entry(symbol: String, epochSecond: Long) = WatchlistEntry(symbol, Instant.ofEpochSecond(epochSecond))

    @Test
    fun `empty watchlist shows the Empty state`() = runTest {
        val viewModel = createViewModel()

        assertEquals(UiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `watched stocks appear newest first, with names before prices arrive`() = runTest {
        watchlist.entries.value = listOf(entry("AAPL", 1), entry("MSFT", 2))

        val viewModel = createViewModel()

        val items = viewModel.items()
        assertEquals(listOf("MSFT", "AAPL"), items.map { it.stock.symbol })
        assertEquals("Microsoft Corporation", items.first().stock.name)
        assertNull(items.first().quote)
    }

    @Test
    fun `live price updates flow into the list`() = runTest {
        watchlist.entries.value = listOf(entry("AAPL", 1))
        val viewModel = createViewModel()

        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "231.00"))
        assertMoney("231.00", viewModel.items().single().quote?.price)

        market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", "232.50"))
        assertMoney("232.50", viewModel.items().single().quote?.price)
    }

    @Test
    fun `unknown symbols are skipped instead of failing the whole list`() = runTest {
        watchlist.entries.value = listOf(entry("AAPL", 1), entry("GONE", 2))

        val viewModel = createViewModel()

        assertEquals(listOf("AAPL"), viewModel.items().map { it.stock.symbol })
    }

    @Test
    fun `undo restores a removed stock in its original position`() = runTest {
        watchlist.entries.value = listOf(entry("AAPL", 1), entry("AMD", 2), entry("MSFT", 3))
        val viewModel = createViewModel()

        viewModel.remove("AMD")
        assertEquals(listOf("MSFT", "AAPL"), viewModel.items().map { it.stock.symbol })

        viewModel.undoRemove()
        assertEquals(listOf("MSFT", "AMD", "AAPL"), viewModel.items().map { it.stock.symbol })
    }

    @Test
    fun `failure shows an error, and retry recovers`() = runTest {
        watchlist.entries.value = listOf(entry("AAPL", 1))
        market.failure = IOException("offline")
        val viewModel = createViewModel()

        assertEquals(UiState.Error(R.string.error_network), viewModel.uiState.value)

        market.failure = null
        viewModel.retry()

        assertEquals(listOf("AAPL"), viewModel.items().map { it.stock.symbol })
    }
}
