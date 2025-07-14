package com.example.konvo.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// --- Shared KonvoTheme data class and theme list ---
data class KonvoTheme(
    val name: String,
    val background: Color,
    val card: Color,
    val groupAccent: Color,
    val dmAccent: Color,
    val unreadBadge: Color,
    val isDark: Boolean,
    val animatedBackground: Boolean = false
) {
    val textPrimary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    val textSecondary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF6B6B6B) else Color(0xFFB0B3C6)
    val messageBubbleMe: Color
        get() = groupAccent
    val messageBubbleOther: Color
        get() = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val inputBackground: Color
        get() = if (isDark) Color(0xFF2A2A2A) else Color.White
    val dialogBackground: Color
        get() = if (background.luminance() > 0.5f) Color.White.copy(alpha = 0.97f) else Color(0xFF18122B)
}

val konvoThemes = listOf(
    KonvoTheme(
        name = "Classic Dark",
        background = Color(0xFF10142A),
        card = Color(0xFF23264A),
        groupAccent = KonvoBlue,
        dmAccent = KonvoOrangeDark,
        unreadBadge = Color(0xFF9B42F2),
        isDark = true
    ),
    KonvoTheme(
        name = "Classic Light",
        background = Color(0xFFF7F8FA),
        card = Color.White,
        groupAccent = KonvoBlueDark,
        dmAccent = KonvoBlue,
        unreadBadge = KonvoBlueDark,
        isDark = false
    ),
    KonvoTheme(
        name = "Neon",
        background = Color(0xFF18122B),
        card = Color(0xFF393053),
        groupAccent = Color(0xFF00FFD0),
        dmAccent = Color(0xFF00B4FF),
        unreadBadge = Color(0xFFFF00E0),
        isDark = true
    ),
    KonvoTheme(
        name = "Pastel",
        background = Color(0xFFF8E8EE),
        card = Color(0xFFFDEBED),
        groupAccent = Color(0xFFB1AFFF),
        dmAccent = Color(0xFFFFB3B3),
        unreadBadge = Color(0xFFB1AFFF),
        isDark = false
    ),
    KonvoTheme(
        name = "Sunset",
        background = Color(0xFFFFE5D9),
        card = Color(0xFFFFB4A2),
        groupAccent = Color(0xFFFF6F61),
        dmAccent = Color(0xFF6B705C),
        unreadBadge = Color(0xFFFF6F61),
        isDark = false
    ),
    // --- New Ultra Aesthetic Galactic Themes ---
    KonvoTheme(
        name = "Galactic Aurora",
        background = Color(0xFF0B0033),
        card = Color(0xFF3700B3),
        groupAccent = Color(0xFF00FFB3),
        dmAccent = Color(0xFF00CFFF),
        unreadBadge = Color(0xFFB388FF),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Cosmic Sunset",
        background = Color(0xFF1A0A2D),
        card = Color(0xFF3D155F),
        groupAccent = Color(0xFFFF6F91),
        dmAccent = Color(0xFFFF9671),
        unreadBadge = Color(0xFFFFC75F),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Stellar Ice",
        background = Color(0xFF0F2027),
        card = Color(0xFF2C5364),
        groupAccent = Color(0xFF36D1C4),
        dmAccent = Color(0xFF5B86E5),
        unreadBadge = Color(0xFFB2FEFA),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Cyberpunk",
        background = Color(0xFF1A1A2E),
        card = Color(0xFF16213E),
        groupAccent = Color(0xFFFF2E63),
        dmAccent = Color(0xFF08D9D6),
        unreadBadge = Color(0xFFFFC300),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Nebula Dream",
        background = Color(0xFF232526),
        card = Color(0xFF414345),
        groupAccent = Color(0xFFDA22FF),
        dmAccent = Color(0xFF9733EE),
        unreadBadge = Color(0xFF56CCF2),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Solar Flare",
        background = Color(0xFFFF512F),
        card = Color(0xFFDD2476),
        groupAccent = Color(0xFFFFC837),
        dmAccent = Color(0xFFFF8008),
        unreadBadge = Color(0xFFFF5F6D),
        isDark = false,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Aurora Borealis",
        background = Color(0xFF232526),
        card = Color(0xFF1CD8D2),
        groupAccent = Color(0xFF93F9B9),
        dmAccent = Color(0xFF1FA2FF),
        unreadBadge = Color(0xFF38F9D7),
        isDark = false,
        animatedBackground = true
    )
)

// Restore LightColors and DarkColors for the default KonvoTheme composable
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
