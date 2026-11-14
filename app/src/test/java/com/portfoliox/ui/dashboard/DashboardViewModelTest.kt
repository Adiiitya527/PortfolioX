package com.portfoliox.ui.dashboard

import com.portfoliox.R
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.FakePortfolioRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class DashboardViewModelTest {

    // @get:Rule applies the annotation to the property's getter, which is where JUnit looks.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePortfolioRepository()

    /**
     * stateIn(WhileSubscribed) only runs the upstream while someone collects, the way the
     * screen does. backgroundScope is cancelled automatically when the test ends.
     */
    @OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher is marked experimental
    private fun TestScope.startCollecting(viewModel: DashboardViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun `state is Loading before anything is collected`() {
        val viewModel = DashboardViewModel(repository)

        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `cash from the repository becomes a Success state`() = runTest {
        val viewModel = DashboardViewModel(repository)
        startCollecting(viewModel)

        assertEquals(UiState.Success("100000.00".toMoney()), viewModel.uiState.value)
    }

    @Test
    fun `state updates when cash changes`() = runTest {
        val viewModel = DashboardViewModel(repository)
        startCollecting(viewModel)

        repository.cash.value = "98500.00".toMoney()

        assertEquals(UiState.Success("98500.00".toMoney()), viewModel.uiState.value)
    }

    @Test
    fun `repository failure becomes an Error state with a user message`() = runTest {
        repository.failure = IOException("disk unavailable")
        val viewModel = DashboardViewModel(repository)
        startCollecting(viewModel)

        assertEquals(UiState.Error(R.string.error_network), viewModel.uiState.value)
    }
}
