package com.portfoliox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = OnBrandBlueContainer,
    secondary = Slate,
    background = LightBackground,
    surface = LightBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = OnBrandBlueDark,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = OnBrandBlueContainerDark,
    secondary = SlateDark,
    background = DarkBackground,
    surface = DarkBackground,
)

/**
 * The app-wide theme. Every screen is wrapped in it (see MainActivity).
 *
 * Dynamic colour (Android 12+ wallpaper-based colours) is deliberately not used:
 * a finance app should look the same on every device, and green/red must stay reliable.
 */
@Composable
fun PortfolioXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
