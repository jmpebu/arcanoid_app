package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ArcadeColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ArcadeBackground,
    primaryContainer = ArcadeSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = ArcadeBackground,
    secondaryContainer = ArcadeSurfaceVariant,
    onSecondaryContainer = NeonPink,
    tertiary = NeonYellow,
    onTertiary = ArcadeBackground,
    background = ArcadeBackground,
    onBackground = ArcadeTextPrimary,
    surface = ArcadeSurface,
    onSurface = ArcadeTextPrimary,
    surfaceVariant = ArcadeSurfaceVariant,
    onSurfaceVariant = ArcadeTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ArcadeBackground.toArgb()
                window.navigationBarColor = ArcadeBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ArcadeColorScheme,
        typography = Typography,
        content = content
    )
}
