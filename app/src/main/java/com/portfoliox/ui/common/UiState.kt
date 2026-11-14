package com.portfoliox.ui.common

import androidx.annotation.StringRes
import com.portfoliox.R
import com.portfoliox.data.market.StockNotFoundException
import java.io.IOException

/**
 * The four states every data-driven screen can be in. A screen renders exactly one of them,
 * and `when (state)` forces it to handle all four.
 *
 * `<out T>` (covariance) means a UiState<Nothing> is also a UiState<List<Stock>>, so the
 * data-less states can be shared singletons.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(val data: T) : UiState<T>

    /** Loaded fine, but there's nothing to show (no holdings, empty watchlist…). */
    data object Empty : UiState<Nothing>

    /**
     * Holds a string resource id rather than text: ViewModels don't have access to resources,
     * and the screen resolves the id with stringResource(), so messages can be translated.
     */
    data class Error(@param:StringRes val messageRes: Int) : UiState<Nothing>
}

/**
 * Converts an exception into a message the user can act on. Technical details belong in logs.
 */
@StringRes
fun Throwable.toUserMessage(): Int = when (this) {
    is StockNotFoundException -> R.string.error_stock_not_found
    is IOException -> R.string.error_network
    else -> R.string.error_generic
}
