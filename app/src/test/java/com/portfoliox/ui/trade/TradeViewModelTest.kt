package com.portfoliox.ui.trade

import com.portfoliox.R
import com.portfoliox.data.repository.TradeResult
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.FakeMarketRepository
import com.portfoliox.testutil.FakePortfolioRepository
import com.portfoliox.testutil.MainDispatcherRule
import com.portfoliox.testutil.TestStocks
import com.portfoliox.testutil.assertMoney
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TradeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository()
    private val portfolio = FakePortfolioRepository()

    private fun TestScope.createViewModel(price: String? = "100.00", cash: String = "1000.00"): TradeViewModel {
        price?.let { market.quotes.value = mapOf("AAPL" to TestStocks.quote("AAPL", it)) }
        portfolio.cash.value = cash.toMoney()
        val viewModel = TradeViewModel("AAPL", market, portfolio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun transaction(type: TradeType, quantity: Int) = Transaction(
        id = 1, symbol = "AAPL", type = type, quantity = quantity,
        price = "100.00".toMoney(), total = (BigDecimal(100) * quantity.toBigDecimal()).toMoney(),
        realizedPnl = null, timestamp = Instant.EPOCH,
    )

    @Test
    fun `new form is a buy with nothing entered and no error shown`() = runTest {
        val state = createViewModel().uiState.value

        assertEquals(TradeType.BUY, state.type)
        assertEquals("", state.quantityText)
        assertNull(state.validationError)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `typing a quantity shows the estimated total at the live price`() = runTest {
        val viewModel = createViewModel(price = "123.45")

        viewModel.onQuantityChange("3")

        assertMoney("370.35", viewModel.uiState.value.estimatedTotal)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `only digits are accepted`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQuantityChange("1a2.5-")

        assertEquals("125", viewModel.uiState.value.quantityText)
    }

    @Test
    fun `buying more than the cash allows shows insufficient cash`() = runTest {
        val viewModel = createViewModel(price = "100.00", cash = "1000.00")

        viewModel.onQuantityChange("11")

        val error = viewModel.uiState.value.validationError as TradeError.InsufficientCash
        assertMoney("1100.00", error.required)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `selling more than owned shows insufficient shares`() = runTest {
        portfolio.holdings.value = listOf(Holding("AAPL", 5, BigDecimal("90")))
        val viewModel = createViewModel()
        viewModel.open(TradeType.SELL)

        viewModel.onQuantityChange("6")

        assertEquals(TradeError.InsufficientShares(requested = 6, owned = 5), viewModel.uiState.value.validationError)
    }

    @Test
    fun `zero shares is invalid`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQuantityChange("0")

        assertEquals(TradeError.InvalidQuantity, viewModel.uiState.value.validationError)
    }

    @Test
    fun `max fills in what the cash can buy, or what is owned when selling`() = runTest {
        portfolio.holdings.value = listOf(Holding("AAPL", 7, BigDecimal("90")))
        val viewModel = createViewModel(price = "300.00", cash = "1000.00")

        viewModel.onUseMax()
        assertEquals("3", viewModel.uiState.value.quantityText) // 1000 / 300 = 3.33 → 3 whole shares

        viewModel.onTypeChange(TradeType.SELL)
        viewModel.onUseMax()
        assertEquals("7", viewModel.uiState.value.quantityText)
    }

    @Test
    fun `nothing can be submitted before a price is available`() = runTest {
        val viewModel = createViewModel(price = null)

        viewModel.onQuantityChange("1")

        assertEquals(TradeError.PriceUnavailable, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `successful submit sends a market order at the live price and reports completion`() = runTest {
        val viewModel = createViewModel(price = "100.00")
        portfolio.nextTradeResult = TradeResult.Success(transaction(TradeType.BUY, 2))
        viewModel.onQuantityChange("2")

        viewModel.submit()

        assertEquals(listOf(TradeOrder("AAPL", TradeType.BUY, 2, "100.00".toMoney())), portfolio.executedOrders)
        assertEquals(2, viewModel.uiState.value.completed?.quantity)

        viewModel.onCompletionHandled()
        assertNull(viewModel.uiState.value.completed)
        assertEquals("", viewModel.uiState.value.quantityText)
    }

    @Test
    fun `rejection at submit time is shown on the form`() = runTest {
        val viewModel = createViewModel()
        val rejection = TradeError.InsufficientCash(required = "200.00".toMoney(), available = "150.00".toMoney())
        portfolio.nextTradeResult = TradeResult.Rejected(rejection)
        viewModel.onQuantityChange("2")

        viewModel.submit()

        assertEquals(rejection, viewModel.uiState.value.rejection)
        assertNull(viewModel.uiState.value.completed)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `unexpected failure shows a message and allows another attempt`() = runTest {
        val viewModel = createViewModel()
        portfolio.tradeFailure = IOException("disk full")
        viewModel.onQuantityChange("1")

        viewModel.submit()

        assertEquals(R.string.error_network, viewModel.uiState.value.failureRes)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `opening the sheet again starts a fresh form`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQuantityChange("4")

        viewModel.open(TradeType.SELL)

        assertEquals(TradeType.SELL, viewModel.uiState.value.type)
        assertEquals("", viewModel.uiState.value.quantityText)
    }
}
