package com.makan.roadpulse.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val RoadPulseDarkColorScheme = darkColorScheme(
    primary              = TealPrimary,
    onPrimary            = BackgroundPrimary,
    primaryContainer     = TealDark,
    onPrimaryContainer   = TealLight,
    secondary            = BluePrimary,
    onSecondary          = BackgroundPrimary,
    secondaryContainer   = BlueDark,
    onSecondaryContainer = BlueLight,
    tertiary             = CoralPrimary,
    onTertiary           = BackgroundPrimary,
    tertiaryContainer    = CoralDark,
    onTertiaryContainer  = CoralLight,
    background           = BackgroundPrimary,
    onBackground         = TextPrimary,
    surface              = BackgroundSecondary,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariantColor,
    onSurfaceVariant     = TextSecondary,
    error                = CoralPrimary,
    onError              = BackgroundPrimary
)

private val RoadPulseLightColorScheme = lightColorScheme(
    primary              = TealActive,
    onPrimary            = Color.White,
    primaryContainer     = TealLight,
    onPrimaryContainer   = TealDark,
    secondary            = BluePrimary,
    onSecondary          = Color.White,
    secondaryContainer   = BlueLight,
    onSecondaryContainer = BlueDark,
    tertiary             = CoralPrimary,
    onTertiary           = Color.White,
    tertiaryContainer    = CoralLight,
    onTertiaryContainer  = CoralDark,
    background           = LightBackground,
    onBackground         = LightTextPrimary,
    surface              = LightSurface,
    onSurface            = LightTextPrimary,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightTextSecondary,
    outline              = Color(0xFFCCCCC4),
    outlineVariant       = Color(0xFFDDDDD6),
    error                = CoralPrimary,
    onError              = Color.White
)

@Composable
fun RoadPulseTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) RoadPulseDarkColorScheme else RoadPulseLightColorScheme
    CompositionLocalProvider(LocalRoadPulseColors provides RoadPulseColors()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = RoadPulseTypography,
            content     = content
        )
    }
}
