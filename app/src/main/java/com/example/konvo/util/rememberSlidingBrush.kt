package com.example.konvo.util



import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun rememberSlidingBrush(
    colors: List<Color> = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF9B42F2)),
    cycleMillis: Int = 2200
): Brush {
    val bandWidth = 1000f
    // Duplicate the color band 3x for a wide seamless gradient
    val seamlessColors = if (colors.size > 1 && colors.first() != colors.last()) colors + colors + colors.first() else colors + colors
    val infinite = rememberInfiniteTransition(label = "slide-gradient")
    val offset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = bandWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    // The visible window is always bandWidth wide, sliding over the seamless gradient
    return Brush.linearGradient(
        colors = seamlessColors,
        start = androidx.compose.ui.geometry.Offset(x = -bandWidth + offset, y = 0f),
        end = androidx.compose.ui.geometry.Offset(x = bandWidth + offset, y = 0f)
    )
}
