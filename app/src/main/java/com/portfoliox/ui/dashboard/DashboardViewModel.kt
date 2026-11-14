package com.portfoliox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.domain.PortfolioCalculator
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.HoldingValuation
import com.portfoliox.domain.model.PortfolioSummary
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.percentOf
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

/** One row in the holdings list. */
data class DashboardHolding(
    val valuation: HoldingValuation,
    /** Company name; falls back to the symbol if the market no longer lists the stock. */
    val name: String,
    /** This holding's share of the total holdings value, in percent. */
    val allocationPercent: BigDecimal,
) {
    val symbol: String get() = valuation.holding.symbol
}

/** Everything the Portfolio tab shows. */
data class DashboardData(
    val summary: PortfolioSummary,
    /** Largest position first. */
    val holdings: List<DashboardHolding>,
)

/**
 * Brings four live sources together:
 *   holdings (Room) ─┬─> distinct symbols ─┬─> live quotes for exactly those symbols
 *                    │                     └─> company names (cached lookup)
 *   cash (Room) ─────┘
 * and runs them through PortfolioCalculator. A price tick, a trade, or a cash change all
 * produce a new dashboard automatically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val portfolioRepository: PortfolioRepository,
    private val marketRepository: MarketRepository,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<DashboardData>> = retryTrigger
        .flatMapLatest {
            dashboardFlow()
                .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { error -> emit(UiState.Error(error.toUserMessage())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun dashboardFlow(): Flow<DashboardData> {
        val holdings = portfolioRepository.observeHoldings()

        // distinctUntilChanged: buying more of a stock you already own changes the holdings
        // list but not the set of symbols, so the price subscription isn't restarted.
        val symbols = holdings
            .map { list -> list.map(Holding::symbol).toSet() }
            .distinctUntilChanged()

        val quotes: Flow<Map<String, Quote>> = symbols.flatMapLatest { set ->
            if (set.isEmpty()) {
                flowOf(emptyMap())
            } else {
                marketRepository.observeQuotes(set).onStart { emit(emptyMap()) }
            }
        }

        val names: Flow<Map<String, String>> = symbols.flatMapLatest { set ->
            flow { emit(marketRepository.getStocks(set).associate { it.symbol to it.name }) }
        }

        return combine(holdings, quotes, names, portfolioRepository.observeCash()) { holdingList, quoteMap, nameMap, cash ->
            val valuations = PortfolioCalculator.valuate(holdingList, quoteMap)
            val summary = PortfolioCalculator.summarize(valuations, cash)
            DashboardData(
                summary = summary,
                holdings = valuations
                    .sortedByDescending { it.marketValue }
                    .map { valuation ->
                        DashboardHolding(
                            valuation = valuation,
                            name = nameMap[valuation.holding.symbol] ?: valuation.holding.symbol,
                            allocationPercent = percentOf(valuation.marketValue, summary.holdingsValue),
                        )
                    },
            )
        }
    }

    fun retry() {
        retryTrigger.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DashboardViewModel(appContainer.portfolioRepository, appContainer.marketRepository)
            }
        }
    }
}
