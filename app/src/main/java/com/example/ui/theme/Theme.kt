package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CalibreColorScheme = darkColorScheme(
    primary = CalibreBeam,
    onPrimary = CalibreBeamDarkText,
    primaryContainer = CalibreBeamDim,
    onPrimaryContainer = CalibreBeam,
    secondary = CalibreOk,
    onSecondary = CalibreBeamDarkText,
    tertiary = CalibreWarn,
    error = CalibreBad,
    background = CalibreBg,
    onBackground = CalibreFg,
    surface = CalibreSurface,
    onSurface = CalibreFg,
    surfaceVariant = CalibreSurface2,
    onSurfaceVariant = CalibreMuted,
    outline = CalibreLine
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Always use the intentional dark room calibration theme as designed in the web app
    MaterialTheme(
        colorScheme = CalibreColorScheme,
        typography = Typography,
        content = content
    )
}
