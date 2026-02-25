package com.example.contactsapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.contactsapp.data.model.AccentColor
import com.example.contactsapp.data.model.AppTheme

@Composable
fun ContactsAppTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    accentColor: AccentColor = AccentColor.INDIGO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = Color(accentColor.hex)
    val accentDim = accent.copy(alpha = 0.7f)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary          = accent,
            primaryContainer = accent.copy(alpha = 0.2f),
            secondary        = accentDim,
            background       = Color(0xFF0D0D0D),
            surface          = Color(0xFF1A1A1A),
            surfaceVariant   = Color(0xFF242424),
            onPrimary        = Color.White,
            onBackground     = Color(0xFFE8E8E8),
            onSurface        = Color(0xFFE8E8E8),
            onSurfaceVariant = Color(0xFFAAAAAA),
            error            = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary          = accent,
            primaryContainer = accent.copy(alpha = 0.12f),
            secondary        = accentDim,
            background       = Color(0xFFF7F7F7),
            surface          = Color(0xFFFFFFFF),
            surfaceVariant   = Color(0xFFF0F0F0),
            onPrimary        = Color.White,
            onBackground     = Color(0xFF111111),
            onSurface        = Color(0xFF111111),
            onSurfaceVariant = Color(0xFF666666),
            error            = Color(0xFFE53935)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
