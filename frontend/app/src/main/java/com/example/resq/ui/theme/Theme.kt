package com.example.resq.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ResQLightColorScheme = lightColorScheme(
    primary          = ResQRed,
    onPrimary        = Color.White,
    primaryContainer = ResQRedLight,
    onPrimaryContainer = Color.White,

    secondary        = StatusGreen,
    onSecondary      = Color.White,

    error            = ResQRed,
    onError          = Color.White,

    background       = BackgroundLight,
    onBackground     = TextPrimary,

    surface          = SurfaceWhite,
    onSurface        = TextPrimary,

    surfaceVariant   = Color(0xFFF0F0F0),
    onSurfaceVariant = TextSecondary,

    outline          = DividerColor,
)

@Composable
fun ResQTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor  = BackgroundLight.toArgb()
            window.navigationBarColor = NavBarBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = ResQLightColorScheme,
        typography  = ResQTypography,
        content     = content,
    )
}
