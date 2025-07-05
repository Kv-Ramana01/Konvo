package com.example.konvo.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary         = KonvoBlue,
    onPrimary       = Color.White,
    primaryContainer= KonvoBlueDark,
    secondary       = KonvoOrange,
    background      = KonvoGrey,
    surfaceVariant  = Color(0xFFE0E4EB)
)

private val DarkColors = darkColorScheme(
    primary         = KonvoOrangeDark,
    onPrimary       = Color.Black,
    primaryContainer= KonvoOrange,
    secondary       = KonvoBlue,
    background      = KonvoNavy,
    surfaceVariant  = KonvoNavyLight
)

@Composable
fun KonvoTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography  = KonvoTypography,
        content     = content
    )
}
