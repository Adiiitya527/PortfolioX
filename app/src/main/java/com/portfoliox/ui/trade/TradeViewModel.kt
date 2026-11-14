package com.portfoliox.ui.trade

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.data.repository.TradeResult
import com.portfoliox.domain.TradeValidator
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import com.portfoliox.domain.model.toMoney
import com.portfoliox.ui.common.appContainer
import com.portfoliox.ui.common.toUserMessage
import com.portfoliox.ui.navigation.StockDetailRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/** What the user has entered or triggered. Only the ViewModel changes it. */
private data class FormInput(
    val type: TradeType = TradeType.BUY,
    val quantityText: String = "",
    val isSubmitting: Boolean = false,
    val completed: Transaction? = null,
    val rejection: TradeError? = null,
    @param:StringRes val failureRes: Int? = null,
)

/**
 * Everything the trade sheet shows. The derived values (quantity, total, validation, maxQuantity)
 * are computed properties, so they always agree with the inputs and live data they come from.
 */
data class TradeUiState(
    val symbol: String,
    val type: TradeType = TradeType.BUY,
    val quantityText: String = "",
    /** Live price; null until the first quote arrives. Trades execute at this price. */
    val quote: Quote? = null,
    val cash: BigDecimal = BigDecimal.ZERO,
    val ownedQuantity: Int = 0,
    val isSubmitting: Boolean = false,
    /** Set when a trade succeeds. The screen closes the sheet, shows a snackbar, then calls onCompletionHandled(). */
    val completed: Transaction? = null,
    /** The repository rejected the trade at submit time (for example, the price moved). */
    val rejection: TradeError? = null,
    /** An unexpected failure, as a message resource. */
    @param:StringRes val failureRes: Int? = null,
) {
    val quantity: Int? get() = quantityText.toIntOrNull()

    val estimatedTotal: BigDecimal?
        get() {
            val price = quote?.price ?: return null
            val shares = quantity ?: return null
            return (price * shares.toBigDecimal()).toMoney()
        }

    /**
     * Live validation as the user types, using the same TradeValidator the repository uses,
     * so the sheet and the database can never disagree about what's allowed.
     * Nothing is shown while the field is still empty.
     */
    val validationError: TradeError?
        get() {
            if (quantityText.isEmpty()) return null
            val shares = quantity ?: return TradeError.InvalidQuantity
            val price = quote?.price ?: return TradeError.PriceUnavailable
            return TradeValidator.validate(TradeOrder(symbol, type, shares, price), cash, ownedQuantity)
        }

    /** The most shares this trade can be for: what the cash buys, or what's owned. */
    val maxQuantity: Int
        get() = when (type) {
            TradeType.BUY -> {
                val price = quote?.price
                if (price == null || price.signum() <= 0) 0 else cash.divide(price, 0, RoundingMode.DOWN).toInt()
            }
            TradeType.SELL -> ownedQuantity
        }

    val canSubmit: Boolean
        get() = !isSubmitting && quote != null && (quantity ?: 0) > 0 && validationError == null
}

/**
 * Holds the trade form. It's scoped to the stock detail screen (same back stack entry), so
 * [open] resets the form each time the sheet is shown.
 *
 * Success is reported through state ([TradeUiState.completed]) rather than a separate event
 * stream: state can't be lost if the screen is recreated at the wrong moment, and the screen
 * acknowledges it with [onCompletionHandled].
 */
class TradeViewModel(
    private val symbol: String,
    marketRepository: MarketRepository,
    private val portfolioRepository: PortfolioRepository,
) : ViewModel() {

    private val input = MutableStateFlow(FormInput())

    val uiState: StateFlow<TradeUiState> = combine(
        input,
        marketRepository.observeQuotes(setOf(symbol)).map<Map<String, Quote>, Quote?> { it[symbol] }.onStart { emit(null) },
        portfolioRepository.observeCash(),
        portfolioRepository.observeHolding(symbol).map { it?.quantity ?: 0 },
    ) { form, quote, cash, owned ->
        TradeUiState(
            symbol = symbol,
            type = form.type,
            quantityText = form.quantityText,
            quote = quote,
            cash = cash,
            ownedQuantity = owned,
            isSubmitting = form.isSubmitting,
            completed = form.completed,
            rejection = form.rejection,
            failureRes = form.failureRes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TradeUiState(symbol))

    /** Starts a fresh form for a buy or a sell. */
    fun open(type: TradeType) {
        input.value = FormInput(type = type)
    }

    fun onTypeChange(type: TradeType) {
        input.update { it.copy(type = type, rejection = null, failureRes = null) }
    }

    /** Keeps digits only (no signs, decimals or pasted text) and caps the length. */
    fun onQuantityChange(text: String) {
        val digits = text.filter(Char::isDigit).take(MAX_QUANTITY_DIGITS)
        input.update { it.copy(quantityText = digits, rejection = null, failureRes = null) }
    }

    fun onUseMax() {
        val max = uiState.value.maxQuantity
        if (max > 0) onQuantityChange(max.toString())
    }

    /**
     * Places a market order at the current live price. The repository re-validates inside its
     * database transaction, so a price move between typing and tapping can't overdraw cash.
     */
    fun submit() {
        val state = uiState.value
        val price = state.quote?.price ?: return
        val shares = state.quantity ?: return
        if (!state.canSubmit) return

        input.update { it.copy(isSubmitting = true, rejection = null, failureRes = null) }
        viewModelScope.launch {
            try {
                when (val result = portfolioRepository.executeTrade(TradeOrder(symbol, state.type, shares, price))) {
                    is TradeResult.Success -> input.update { it.copy(isSubmitting = false, completed = result.transaction) }
                    is TradeResult.Rejected -> input.update { it.copy(isSubmitting = false, rejection = result.error) }
                }
            } catch (e: CancellationException) {
                throw e // never swallow cancellation: it's how coroutines stop
            } catch (e: Exception) {
                input.update { it.copy(isSubmitting = false, failureRes = e.toUserMessage()) }
            }
        }
    }

    /** Called by the screen after it has reacted to a completed trade. */
    fun onCompletionHandled() {
        input.value = FormInput()
    }

    companion object {
        private const val MAX_QUANTITY_DIGITS = 6

        val Factory = viewModelFactory {
            initializer {
                TradeViewModel(
                    symbol = createSavedStateHandle().toRoute<StockDetailRoute>().symbol,
                    marketRepository = appContainer.marketRepository,
                    portfolioRepository = appContainer.portfolioRepository,
                )
            }
        }
    }
}
