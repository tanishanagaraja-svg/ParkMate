package com.example.loopark.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkNavyColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = DarkNavyBackground,
    primaryContainer = AccentGreenContainer,
    onPrimaryContainer = AccentGreenLight,
    secondary = AccentGreenLight,
    onSecondary = DarkNavyBackground,
    secondaryContainer = DarkNavySurfaceElevated,
    onSecondaryContainer = TextLight,
    tertiary = AccentGreen,
    onTertiary = DarkNavyBackground,
    background = DarkNavyBackground,
    onBackground = TextWhite,
    surface = DarkNavySurface,
    onSurface = TextWhite,
    surfaceVariant = DarkNavySurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = DarkNavyBorder
)

// For hackathon consistency, both default to dark navy & green aesthetic
private val LightNavyColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = DarkNavyBackground,
    primaryContainer = AccentGreenContainer,
    onPrimaryContainer = AccentGreenLight,
    secondary = AccentGreenLight,
    onSecondary = DarkNavyBackground,
    secondaryContainer = DarkNavySurfaceElevated,
    onSecondaryContainer = TextLight,
    tertiary = AccentGreen,
    onTertiary = DarkNavyBackground,
    background = DarkNavyBackground,
    onBackground = TextWhite,
    surface = DarkNavySurface,
    onSurface = TextWhite,
    surfaceVariant = DarkNavySurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = DarkNavyBorder
)

@Composable
fun ParkMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkNavyColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for the default template name
@Composable
fun LooParkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ParkMateTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
