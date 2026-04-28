package com.whc06.trainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFFFF6B35)
private val AccentDark = Color(0xFFFF8B57)
private val BgDark = Color(0xFF0E1014)
private val SurfaceDark = Color(0xFF181B22)
private val TextDark = Color(0xFFEFEFEF)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.Black,
    secondary = Color(0xFF5BC0EB),
    background = BgDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFF20242C),
    onSurfaceVariant = Color(0xFFB7BBC2)
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Color(0xFF1A8FE3),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF6F7FA),
    onSurface = Color(0xFF111111)
)

@Composable
fun WhC06TrainerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

@Composable
fun GripperTheme(
    mode: com.whc06.trainer.data.ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        com.whc06.trainer.data.ThemeMode.SYSTEM -> systemDark
        com.whc06.trainer.data.ThemeMode.DARK -> true
        com.whc06.trainer.data.ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
