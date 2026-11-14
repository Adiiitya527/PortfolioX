package com.portfoliox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 has no built-in "profit" and "loss" colours, so we add our own.
 *
 * @Immutable promises Compose these values never change after creation,
 * which lets it skip unnecessary redraws.
 */
@Immutable
data class FinancialColors(
    val gain: Color,
    val loss: Color,
)

internal val LightFinancialColors = FinancialColors(gain = GainLight, loss = LossLight)
internal val DarkFinancialColors = FinancialColors(gain = GainDark, loss = LossDark)

/**
 * A CompositionLocal passes a value down the UI tree without threading it through every
 * function parameter. Web analogy: React Context. PortfolioXTheme "provides" it and
 * any composable below can read it.
 */
internal val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

/**
 * Extension property so screens can write `MaterialTheme.financialColors.gain`,
 * reading exactly like the built-in `MaterialTheme.colorScheme.primary`.
 */
val MaterialTheme.financialColors: FinancialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinancialColors.current
