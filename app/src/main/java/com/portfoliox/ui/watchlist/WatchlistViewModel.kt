package com.portfoliox.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfoliox.data.market.StockNotFoundException
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.WatchlistRepository
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.domain.model.WatchlistEntry
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One watchlist row. [quote] is null until the first live price arrives. */
data class WatchlistItem(
    val stock: Stock,
    val quote: Quote?,
)

/**
 * Combines two sources into one list: which symbols are watched (Room) and their live prices
 * (market data). Either changing produces a new list for the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest is still marked experimental
class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
) : ViewModel() {

    /** Incremented by retry(). Each new value restarts the pipeline below from scratch. */
    private val retryTrigger = MutableStateFlow(0)

    /** The most recently removed entry, kept so the user can undo. */
    private var lastRemoved: WatchlistEntry? = null

    /**
     * flatMapLatest: when the upstream emits a new value, cancel the work started for the old
     * value and start again. When the watchlist changes, we stop listening to the old symbols'
     * prices and subscribe to the new set. Web analogy: RxJS switchMap.
     */
    val uiState: StateFlow<UiState<List<WatchlistItem>>> = retryTrigger
        .flatMapLatest {
            watchlistRepository.observeEntries()
                .flatMapLatest { entries -> itemsFor(entries) }
                .onStart { emit(UiState.Loading) }
                .catch { error -> emit(UiState.Error(error.toUserMessage())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun itemsFor(entries: List<WatchlistEntry>): Flow<UiState<List<WatchlistItem>>> {
        if (entries.isEmpty()) return flow { emit(UiState.Empty) }

        return flow {
            val stocks = loadStocks(entries.map { it.symbol })
            val quotes = marketRepository.observeQuotes(stocks.map { it.symbol }.toSet())
                .onStart { emit(emptyMap()) } // show names immediately, prices fill in on the first tick
            emitAll(
                quotes.map { quoteMap ->
                    UiState.Success(stocks.map { WatchlistItem(it, quoteMap[it.symbol]) })
                },
            )
        }
    }

    /**
     * Loads stock details for all symbols in parallel: `async` starts each request, `awaitAll`
     * waits for every one. With 10 symbols at 300 ms each, that's ~300 ms instead of 3 s.
     * coroutineScope ensures that if one fails, the others are cancelled (structured concurrency).
     */
    private suspend fun loadStocks(symbols: List<String>): List<Stock> = coroutineScope {
        symbols
            .map { symbol -> async { findStock(symbol) } }
            .awaitAll()
            .filterNotNull() // a symbol no longer offered by the market is skipped, not fatal
    }

    private suspend fun findStock(symbol: String): Stock? =
        try {
            marketRepository.getStock(symbol)
        } catch (_: StockNotFoundException) {
            null
        }

    fun retry() {
        retryTrigger.value++
    }

    /**
     * Removes immediately and remembers the entry (with its original date) for [undoRemove].
     * `first()` takes the current value of the Flow once, then stops collecting.
     */
    fun remove(symbol: String) {
        viewModelScope.launch {
            lastRemoved = watchlistRepository.observeEntries().first().firstOrNull { it.symbol == symbol }
            watchlistRepository.remove(symbol)
        }
    }

    fun undoRemove() {
        val entry = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch { watchlistRepository.restore(entry) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WatchlistViewModel(appContainer.watchlistRepository, appContainer.marketRepository)
            }
        }
    }
}
