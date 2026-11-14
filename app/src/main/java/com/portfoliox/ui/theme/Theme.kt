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
    inversePrimary = BrandBlueDark, // accent on dark "inverse" surfaces, e.g. the snackbar's action
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = OnBrandBlueContainer,
    secondary = Slate,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = LightBackground,
    surface = LightBackground,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = OnBrandBlueDark,
    inversePrimary = BrandBlue,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = OnBrandBlueContainerDark,
    secondary = SlateDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = DarkBackground,
    surface = DarkBackground,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
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
