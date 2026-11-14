package com.portfoliox.ui.stockdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.data.repository.WatchlistRepository
import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import com.portfoliox.ui.navigation.StockDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the screen shows above the chart. */
data class StockDetail(
    val stock: Stock,
    /** Null until the first live price arrives. */
    val quote: Quote?,
    val isWatched: Boolean,
    /** The user's position in this stock, or null if they don't own any. */
    val position: HoldingValuation?,
)

/**
 * Two independent pipelines:
 * - [uiState]: stock info + live quote + watchlist star + position (header, stats, position card)
 * - [chartState]: price history for the selected range
 * Keeping them apart means switching 1M → 1Y reloads only the chart, and a chart error
 * doesn't hide the rest of the screen.
 *
 * @param symbol comes from the navigation route; see [Factory].
 * @param savedStateHandle survives process death (it's saved into the Activity's saved
 * instance state). A ViewModel alone only survives configuration changes such as rotation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailViewModel(
    val symbol: String,
    private val marketRepository: MarketRepository,
    private val watchlistRepository: WatchlistRepository,
    private val portfolioRepository: PortfolioRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    /** The chosen chart range, restored if Android killed the app in the background. */
    private val selectedRange: StateFlow<ChartRange> = savedStateHandle.getStateFlow(KEY_RANGE, DEFAULT_RANGE)
    private val chartRetryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<StockDetail>> = retryTrigger
        .flatMapLatest {
            flow {
                val stock = marketRepository.getStock(symbol)
                val quote = marketRepository.observeQuotes(setOf(symbol))
                    .map { quotes -> quotes[symbol] }
                    .onStart { emit(null) }

                // Three live sources → one screen state. Any of them changing re-emits.
                emitAll(
                    combine(
                        quote,
                        watchlistRepository.observeIsWatched(symbol),
                        portfolioRepository.observeHolding(symbol),
                    ) { latestQuote, isWatched, holding ->
                        val position = holding?.let {
                            if (latestQuote != null) HoldingValuation.from(it, latestQuote) else HoldingValuation.atCost(it)
                        }
                        UiState.Success(StockDetail(stock, latestQuote, isWatched, position))
                    },
                )
            }
                .onStart<UiState<StockDetail>> { emit(UiState.Loading) }
                .catch { error -> emit(UiState.Error(error.toUserMessage())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val chartState: StateFlow<ChartUiState> =
        combine(selectedRange, chartRetryTrigger) { range, _ -> range }
            .flatMapLatest { range ->
                flow {
                    emit(ChartUiState(range, UiState.Loading))
                    val points = marketRepository.getPriceHistory(symbol, range)
                    emit(ChartUiState(range, if (points.isEmpty()) UiState.Empty else UiState.Success(points)))
                }.catch { error -> emit(ChartUiState(range, UiState.Error(error.toUserMessage()))) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartUiState(DEFAULT_RANGE, UiState.Loading))

    fun selectRange(range: ChartRange) {
        savedStateHandle[KEY_RANGE] = range // enums are Serializable, so they can be saved
    }

    fun retry() {
        retryTrigger.value++
    }

    fun retryChart() {
        chartRetryTrigger.value++
    }

    fun toggleWatchlist() {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            if (current.isWatched) watchlistRepository.remove(symbol) else watchlistRepository.add(symbol)
        }
    }

    companion object {
        val DEFAULT_RANGE = ChartRange.MONTH
        private const val KEY_RANGE = "chart_range"

        /**
         * createSavedStateHandle() gives the navigation arguments of this screen's back stack
         * entry; toRoute() decodes them into our typed StockDetailRoute. Reading the symbol here
         * (instead of inside the ViewModel) keeps the ViewModel a plain class that tests can
         * construct with any symbol.
         */
        val Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                StockDetailViewModel(
                    symbol = savedStateHandle.toRoute<StockDetailRoute>().symbol,
                    marketRepository = appContainer.marketRepository,
                    watchlistRepository = appContainer.watchlistRepository,
                    portfolioRepository = appContainer.portfolioRepository,
                    savedStateHandle = savedStateHandle,
                )
            }
        }
    }
}
