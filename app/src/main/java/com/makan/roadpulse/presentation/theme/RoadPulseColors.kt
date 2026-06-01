package com.makan.roadpulse.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class RoadPulseColors(
    val backgroundPrimary: Color   = BackgroundPrimary,
    val backgroundSecondary: Color = BackgroundSecondary,
    val tealPrimary: Color  = TealPrimary,
    val tealLight: Color    = TealLight,
    val tealDark: Color     = TealDark,
    val tealActive: Color   = TealActive,
    val bluePrimary: Color  = BluePrimary,
    val blueLight: Color    = BlueLight,
    val blueDark: Color     = BlueDark,
    val coralPrimary: Color = CoralPrimary,
    val coralLight: Color   = CoralLight,
    val coralDark: Color    = CoralDark,
    val textPrimary: Color   = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color  = TextTertiary,
    val navInactive: Color   = NavInactive
)

val LocalRoadPulseColors = staticCompositionLocalOf { RoadPulseColors() }

val roadPulseColors: RoadPulseColors
    @Composable get() = LocalRoadPulseColors.current
