package com.manursan.seguimientokm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.manursan.seguimientokm.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** Paleta viva de la app: cada sección tiene su color. */
object Palette {
    val blue = Color(0xFF2F6BFF)
    val teal = Color(0xFF00B8A9)
    val green = Color(0xFF34B36B)
    val orange = Color(0xFFFF7A1A)
    val purple = Color(0xFF8B5CF6)
    val pink = Color(0xFFF0508C)
    val amber = Color(0xFFFFB300)
    val red = Color(0xFFE5484D)
    val indigo = Color(0xFF4F46E5)
}

private val LightScheme = lightColorScheme(
    primary = Palette.blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B2A6F),
    secondary = Palette.orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3CF),
    onSecondaryContainer = Color(0xFF5A2600),
    tertiary = Palette.purple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBE3FF),
    onTertiaryContainer = Color(0xFF2E1065),
    background = Color(0xFFF3F5FB),
    onBackground = Color(0xFF1B1F2B),
    surface = Color.White,
    onSurface = Color(0xFF1B1F2B),
    surfaceVariant = Color(0xFFE6EAF4),
    onSurfaceVariant = Color(0xFF555C6E),
    outlineVariant = Color(0xFFD5DAE6),
    error = Palette.red,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FB0FF),
    onPrimary = Color(0xFF00287A),
    primaryContainer = Color(0xFF1E3F9E),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFFFB27A),
    onSecondary = Color(0xFF4A1E00),
    secondaryContainer = Color(0xFF7A3A00),
    onSecondaryContainer = Color(0xFFFFE3CF),
    tertiary = Color(0xFFC4B0FF),
    onTertiary = Color(0xFF2E1065),
    background = Color(0xFF111420),
    onBackground = Color(0xFFE6E9F2),
    surface = Color(0xFF1A1E2C),
    onSurface = Color(0xFFE6E9F2),
    surfaceVariant = Color(0xFF2A3042),
    onSurfaceVariant = Color(0xFFB4BACB),
    outlineVariant = Color(0xFF3A4157),
    error = Color(0xFFFF8A8E),
)

/** true si la app está en oscuro (por preferencia o por el sistema). */
val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun positiveColor(): Color = if (LocalDarkTheme.current) Color(0xFF7FE0A5) else Color(0xFF1E8E4E)

@Composable
fun negativeColor(): Color = if (LocalDarkTheme.current) Color(0xFFFF9BA0) else Palette.red

/** Color de acento suavizado para fondos de tarjeta (tinte sobre la superficie). */
@Composable
fun tint(accent: Color, alpha: Float = if (LocalDarkTheme.current) 0.22f else 0.10f): Color =
    accent.copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surface)

/** Versión del acento legible como texto sobre el tinte. */
@Composable
fun accentText(accent: Color): Color =
    if (LocalDarkTheme.current) accent.copy(alpha = 0.55f).compositeOver(Color.White) else accent.copy(alpha = 0.85f).compositeOver(Color.Black)

@Composable
fun SeguimientoKmTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
