package com.portfoliox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.ui.common.UiState
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

/**
 * Holds the Portfolio screen's state. For now it shows the cash balance; Phase 7 extends it
 * to the full summary and holdings.
 *
 * A ViewModel survives configuration changes (rotation, dark-mode switch) while the screen
 * is rebuilt, so data isn't reloaded each time. Web analogy: state kept in a store outside
 * the component, rather than in the component's own useState.
 */
class DashboardViewModel(
    portfolioRepository: PortfolioRepository,
) : ViewModel() {

    /**
     * The pipeline: Room Flow → map to UiState → catch errors → StateFlow for the UI.
     *
     * stateIn turns the cold Flow into a hot StateFlow that always has a current value
     * (starting with Loading). WhileSubscribed(5_000) keeps the upstream running for 5 seconds
     * after the screen stops observing, so a quick rotation doesn't restart the query, but a
     * screen left in the background stops using resources.
     */
    val uiState: StateFlow<UiState<BigDecimal>> = portfolioRepository.observeCash()
        .map<BigDecimal, UiState<BigDecimal>> { cash -> UiState.Success(cash) }
        .catch { error -> emit(UiState.Error(error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    companion object {
        /** Tells Android how to build this ViewModel with its dependency from AppContainer. */
        val Factory = viewModelFactory {
            initializer { DashboardViewModel(appContainer.portfolioRepository) }
        }
    }
}
