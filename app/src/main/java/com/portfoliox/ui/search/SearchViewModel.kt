package com.portfoliox.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.WatchlistRepository
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One search result row. */
data class SearchResult(
    val stock: Stock,
    val quote: Quote?,
    val isWatched: Boolean,
)

/**
 * Search pipeline:
 * query text → debounce (wait for typing to pause) → search → live prices + watchlist stars.
 *
 * The text field's own state lives in the screen. The ViewModel only receives the query
 * through [onQueryChange], which keeps typing instant regardless of what the pipeline is doing.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class) // debounce / flatMapLatest
class SearchViewModel(
    private val marketRepository: MarketRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val retryTrigger = MutableStateFlow(0)

    /**
     * debounce(300): only searches after 300 ms without a new keystroke, so typing "NVDA"
     * runs one search instead of four. Web analogy: lodash.debounce on an input handler.
     *
     * flatMapLatest cancels a still-running search as soon as a newer query arrives, so
     * slow, out-of-date results can never overwrite newer ones.
     */
    val uiState: StateFlow<UiState<List<SearchResult>>> =
        combine(query.debounce(DEBOUNCE_MILLIS), retryTrigger) { text, _ -> text.trim() }
            .flatMapLatest { text -> resultsFor(text) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun resultsFor(text: String): Flow<UiState<List<SearchResult>>> = flow {
        val stocks = marketRepository.searchStocks(text)
        if (stocks.isEmpty()) {
            emit(UiState.Empty)
            return@flow // `return@flow` leaves just this lambda (a labelled return)
        }

        val quotes = marketRepository.observeQuotes(stocks.map { it.symbol }.toSet())
            .onStart { emit(emptyMap()) }
        val watched = watchlistRepository.observeEntries()
            .map { entries -> entries.map { it.symbol }.toSet() }

        // combine: emit whenever prices OR the watchlist change, using the latest of each.
        emitAll(
            combine(quotes, watched) { quoteMap, watchedSymbols ->
                UiState.Success(
                    stocks.map { stock ->
                        SearchResult(stock, quoteMap[stock.symbol], isWatched = stock.symbol in watchedSymbols)
                    },
                )
            },
        )
    }
        .onStart { emit(UiState.Loading) }
        .catch { error -> emit(UiState.Error(error.toUserMessage())) }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun retry() {
        retryTrigger.value++
    }

    fun toggleWatchlist(result: SearchResult) {
        viewModelScope.launch {
            if (result.isWatched) {
                watchlistRepository.remove(result.stock.symbol)
            } else {
                watchlistRepository.add(result.stock.symbol)
            }
        }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 300L

        val Factory = viewModelFactory {
            initializer {
                SearchViewModel(appContainer.marketRepository, appContainer.watchlistRepository)
            }
        }
    }
}
