package com.example.urldownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Brand palette ────────────────────────────────────────────────────────────
object AppColors {
    val Purple      = Color(0xFF7C4DFF)
    val PurpleLight = Color(0xFFB47CFF)
    val Blue        = Color(0xFF3D5AFE)
    val Cyan        = Color(0xFF00BCD4)
    val CyanLight   = Color(0xFF62EFFF)
    val Teal        = Color(0xFF1DE9B6)
    val Orange      = Color(0xFFFF6D00)
    val Red         = Color(0xFFFF1744)
    val Green       = Color(0xFF00E676)

    // Dark surface layers
    val Surface0 = Color(0xFF0D0F1A)   // background
    val Surface1 = Color(0xFF141625)   // surface
    val Surface2 = Color(0xFF1C1F32)   // card
    val Surface3 = Color(0xFF242840)   // elevated card

    // Light surface layers
    val LightBg      = Color(0xFFF5F3FF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightCard    = Color(0xFFF0EEFF)
}

private val DarkScheme = darkColorScheme(
    primary             = AppColors.PurpleLight,
    onPrimary           = Color(0xFF21005D),
    primaryContainer    = Color(0xFF42008F),
    onPrimaryContainer  = Color(0xFFEADDFF),
    secondary           = AppColors.CyanLight,
    onSecondary         = Color(0xFF00363D),
    secondaryContainer  = Color(0xFF004F59),
    onSecondaryContainer= Color(0xFF9EEFFD),
    tertiary            = AppColors.Teal,
    onTertiary          = Color(0xFF003730),
    tertiaryContainer   = Color(0xFF005047),
    onTertiaryContainer = Color(0xFF6FF7E8),
    error               = AppColors.Red,
    errorContainer      = Color(0xFF93000A),
    onError             = Color(0xFF690005),
    onErrorContainer    = Color(0xFFFFDAD6),
    background          = AppColors.Surface0,
    onBackground        = Color(0xFFE6E1F9),
    surface             = AppColors.Surface1,
    onSurface           = Color(0xFFE6E1F9),
    surfaceVariant      = AppColors.Surface2,
    onSurfaceVariant    = Color(0xFFCAC4DC),
    outline             = Color(0xFF958DA5),
    outlineVariant      = Color(0xFF4A4558),
    inverseSurface      = Color(0xFFE6E1F9),
    inverseOnSurface    = AppColors.Surface0,
    inversePrimary      = AppColors.Purple,
)

private val LightScheme = lightColorScheme(
    primary             = AppColors.Purple,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFEADDFF),
    onPrimaryContainer  = Color(0xFF21005D),
    secondary           = AppColors.Cyan,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFCFF9FF),
    onSecondaryContainer= Color(0xFF001F24),
    tertiary            = Color(0xFF00897B),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF00251E),
    error               = Color(0xFFB3261E),
    errorContainer      = Color(0xFFF9DEDC),
    onError             = Color.White,
    onErrorContainer    = Color(0xFF410E0B),
    background          = AppColors.LightBg,
    onBackground        = Color(0xFF1C1B1F),
    surface             = AppColors.LightSurface,
    onSurface           = Color(0xFF1C1B1F),
    surfaceVariant      = AppColors.LightCard,
    onSurfaceVariant    = Color(0xFF49454F),
    outline             = Color(0xFF79747E),
    outlineVariant      = Color(0xFFCAC4D0),
    inverseSurface      = Color(0xFF313033),
    inverseOnSurface    = Color(0xFFF4EFF4),
    inversePrimary      = AppColors.PurpleLight,
)

@Composable
fun URLDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else      -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
