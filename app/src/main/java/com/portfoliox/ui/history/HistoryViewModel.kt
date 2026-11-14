package com.portfoliox.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.domain.model.toMoney
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/**
 * Transaction history from Room, filtered and grouped by day.
 *
 * @param clock supplies the time zone and "today". Injected so tests don't depend on where or
 * when they run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val portfolioRepository: PortfolioRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<HistoryData>> = retryTrigger
        .flatMapLatest {
            combine(portfolioRepository.observeTransactions(), filter) { transactions, selected ->
                if (transactions.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(
                        HistoryData(
                            filter = selected,
                            days = groupByDay(transactions.filter(selected::matches), clock.zone),
                            // mapNotNull drops buys (null realized P&L); sumOf adds BigDecimals exactly.
                            totalRealizedPnl = transactions.mapNotNull { it.realizedPnl }.sumOf { it }.toMoney(),
                            tradeCount = transactions.size,
                            today = LocalDate.now(clock),
                        ),
                    )
                }
            }
                .onStart<UiState<HistoryData>> { emit(UiState.Loading) }
                .catch { error -> emit(UiState.Error(error.toUserMessage())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun selectFilter(selected: HistoryFilter) {
        filter.value = selected
    }

    fun retry() {
        retryTrigger.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoryViewModel(appContainer.portfolioRepository) }
        }
    }
}
