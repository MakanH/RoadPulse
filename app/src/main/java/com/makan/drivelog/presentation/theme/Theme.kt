package com.makan.drivelog.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DriveLogColorScheme = darkColorScheme(
    primary             = TealPrimary,
    onPrimary           = BackgroundPrimary,
    primaryContainer    = TealDark,
    onPrimaryContainer  = TealLight,
    secondary           = BluePrimary,
    onSecondary         = BackgroundPrimary,
    secondaryContainer  = BlueDark,
    onSecondaryContainer = BlueLight,
    tertiary            = CoralPrimary,
    onTertiary          = BackgroundPrimary,
    tertiaryContainer   = CoralDark,
    onTertiaryContainer = CoralLight,
    background          = BackgroundPrimary,
    onBackground        = TextPrimary,
    surface             = BackgroundSecondary,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceVariantColor,
    onSurfaceVariant    = TextSecondary,
    error               = CoralPrimary,
    onError             = BackgroundPrimary
)

@Composable
fun DriveLogTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDriveLogColors provides DriveLogColors()) {
        MaterialTheme(
            colorScheme = DriveLogColorScheme,
            typography  = DriveLogTypography,
            content     = content
        )
    }
}
