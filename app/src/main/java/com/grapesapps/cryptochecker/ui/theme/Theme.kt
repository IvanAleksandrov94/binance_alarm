package com.grapesapps.cryptochecker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200
)

@Composable
fun CryptoCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette

    } else {
        LightColorPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity

            if (darkTheme) {
                activity.window.navigationBarColor =
                    DarkColorPalette.surface.copy(alpha = 0.08f).compositeOver(DarkColorPalette.surface.copy()).toArgb()
                activity.window.statusBarColor = DarkColorPalette.surface.toArgb()
            } else {
                activity.window.navigationBarColor =
                    LightColorPalette.surface.copy(alpha = 0.08f).compositeOver(LightColorPalette.surface.copy())
                        .toArgb()
                activity.window.statusBarColor = LightColorPalette.surface.toArgb()
            }

            WindowCompat.getInsetsController(activity.window, view)?.isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(activity.window, view)?.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}