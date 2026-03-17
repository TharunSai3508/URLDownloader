package com.example.urldownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF82AAFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003258),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF00497C),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD3E4FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBBC7DB),
    background = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE2E2E6),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2E2E6),
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF005FAF),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD3E4FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001C3A),
    secondary = androidx.compose.ui.graphics.Color(0xFF545F71),
    background = androidx.compose.ui.graphics.Color(0xFFFAFCFF),
    surface = androidx.compose.ui.graphics.Color(0xFFFAFCFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
)

@Composable
fun URLDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
