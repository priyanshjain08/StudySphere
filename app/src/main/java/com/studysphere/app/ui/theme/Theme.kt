package com.studysphere.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7CF6), onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2466), onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF4F9DFF), onSecondary = Color.White,
    secondaryContainer = Color(0xFF16345E), onSecondaryContainer = Color(0xFFD6E8FF),
    tertiary = Color(0xFFFBBF24), onTertiary = Color(0xFF1A1300),
    background = Color(0xFF0B1026), onBackground = Color(0xFFEDEFFF),
    surface = Color(0xFF131A3A), onSurface = Color(0xFFEDEFFF),
    surfaceVariant = Color(0xFF1D2652), onSurfaceVariant = Color(0xFFB4BAE0),
    surfaceContainerLow = Color(0xFF131A3A), surfaceContainer = Color(0xFF162043),
    surfaceContainerHigh = Color(0xFF1B2650), surfaceContainerHighest = Color(0xFF1D2652),
    outline = Color(0xFF3A447A), error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BDB), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF), onPrimaryContainer = Color(0xFF1B1257),
    secondary = Color(0xFF1F6FDB), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8FF), onSecondaryContainer = Color(0xFF0A2447),
    tertiary = Color(0xFF9A6A00), onTertiary = Color.White,
    background = Color(0xFFF5F6FF), onBackground = Color(0xFF11152E),
    surface = Color.White, onSurface = Color(0xFF11152E),
    surfaceVariant = Color(0xFFE9EBFA), onSurfaceVariant = Color(0xFF4B5073),
    surfaceContainerLow = Color.White, surfaceContainer = Color(0xFFF0F1FC),
    surfaceContainerHigh = Color(0xFFEAECFA), surfaceContainerHighest = Color(0xFFE4E6F7),
    outline = Color(0xFFB4B9DA), error = Color(0xFFD32F2F)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)
)

/** themeMode: 0 system, 1 light, 2 dark (default). */
@Composable
fun StudySphereTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 0 -> isSystemInDarkTheme(); else -> true }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, shapes = AppShapes, content = content)
}
