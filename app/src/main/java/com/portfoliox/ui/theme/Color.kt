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

// Brand (dark)
internal val BrandBlueDark = Color(0xFFB4C5FF)
internal val OnBrandBlueDark = Color(0xFF00297A)
internal val BrandBlueContainerDark = Color(0xFF003BAB)
internal val OnBrandBlueContainerDark = Color(0xFFDCE4FF)
internal val SlateDark = Color(0xFFBFC6DC)
internal val DarkBackground = Color(0xFF111318)

// Profit / loss
internal val GainLight = Color(0xFF0B8043)
internal val LossLight = Color(0xFFC62828)
internal val GainDark = Color(0xFF5BD68A)
internal val LossDark = Color(0xFFFF8A80)
