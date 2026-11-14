package com.portfoliox.ui.theme

import androidx.compose.ui.graphics.Color

// Raw colour values. Screens never use these directly; they read them through
// MaterialTheme.colorScheme or MaterialTheme.financialColors so light/dark mode works automatically.

// Brand (light)
internal val BrandBlue = Color(0xFF1F4FD1)
internal val BrandBlueContainer = Color(0xFFDCE4FF)
internal val OnBrandBlueContainer = Color(0xFF00174D)
internal val Slate = Color(0xFF565E71)
internal val LightBackground = Color(0xFFF8F9FC)
internal val SecondaryContainerLight = Color(0xFFDCE2F9)
internal val OnSecondaryContainerLight = Color(0xFF141B2C)

// Neutral blue-grey surfaces (light). Without these, Material 3 falls back to its
// default purple-tinted palette for cards, sheets and the navigation bar.
internal val SurfaceVariantLight = Color(0xFFE1E2EC)
internal val OnSurfaceVariantLight = Color(0xFF44474F)
internal val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLowLight = Color(0xFFF2F3F8)
internal val SurfaceContainerLight = Color(0xFFECEEF3)
internal val SurfaceContainerHighLight = Color(0xFFE6E8EE)
internal val SurfaceContainerHighestLight = Color(0xFFE0E2E8)
internal val OutlineLight = Color(0xFF74777F)
internal val OutlineVariantLight = Color(0xFFC4C6D0)

// Brand (dark)
internal val BrandBlueDark = Color(0xFFB4C5FF)
internal val OnBrandBlueDark = Color(0xFF00297A)
internal val BrandBlueContainerDark = Color(0xFF003BAB)
internal val OnBrandBlueContainerDark = Color(0xFFDCE4FF)
internal val SlateDark = Color(0xFFBFC6DC)
internal val DarkBackground = Color(0xFF111318)
internal val SecondaryContainerDark = Color(0xFF3E4759)
internal val OnSecondaryContainerDark = Color(0xFFDAE2F9)

// Neutral blue-grey surfaces (dark)
internal val SurfaceVariantDark = Color(0xFF44474F)
internal val OnSurfaceVariantDark = Color(0xFFC4C6D0)
internal val SurfaceContainerLowestDark = Color(0xFF0C0E13)
internal val SurfaceContainerLowDark = Color(0xFF191C20)
internal val SurfaceContainerDark = Color(0xFF1D2024)
internal val SurfaceContainerHighDark = Color(0xFF282A2F)
internal val SurfaceContainerHighestDark = Color(0xFF33353A)
internal val OutlineDark = Color(0xFF8E9099)
internal val OutlineVariantDark = Color(0xFF44474F)

// Profit / loss
internal val GainLight = Color(0xFF0B8043)
internal val LossLight = Color(0xFFC62828)
internal val GainDark = Color(0xFF5BD68A)
internal val LossDark = Color(0xFFFF8A80)
