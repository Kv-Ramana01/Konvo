package com.example.konvo.util

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun rememberCosmicBrushes(
    colors: List<Color> = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF9B42F2)),
    cycleMillis: Int = 2200,
    parallax: Offset = Offset.Zero,
    windowSize: IntSize? = null
): Pair<Brush, Brush> {
    val bandWidth = 2000f
    val margin = 600f
    val seamlessColors = if (colors.size > 1 && colors.first() != colors.last()) colors + colors + colors.first() else colors + colors

    // Animate diagonal offset for layer 1
    val infinite1 = rememberInfiniteTransition(label = "slide-gradient-1")
    val offset1 by infinite1.animateFloat(
        initialValue = 0f,
        targetValue = bandWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset1"
    )
    val morphProgress1 by infinite1.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMillis * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_morph1"
    )
    val morphedColors1 = seamlessColors.mapIndexed { i, c ->
        val target = seamlessColors[seamlessColors.size - 1 - i]
        lerp(c, target, morphProgress1)
    }
    val brush1 = Brush.linearGradient(
        colors = morphedColors1,
        start = Offset(x = -margin + offset1 + parallax.x, y = -margin + offset1/2 + parallax.y),
        end = Offset(x = bandWidth + margin + offset1 + parallax.x, y = bandWidth/2 + margin + offset1/2 + parallax.y)
    )

    // Layer 2: different color set, speed, and direction
    val altColors = listOf(Color.White.copy(alpha = 0.12f)) + colors.reversed().map { it.copy(alpha = 0.5f) }
    val infinite2 = rememberInfiniteTransition(label = "slide-gradient-2")
    val offset2 by infinite2.animateFloat(
        initialValue = 0f,
        targetValue = bandWidth,
        animationSpec = infiniteRepeatable(
            animation = tween((cycleMillis * 3.2).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset2"
    )
    val morphProgress2 by infinite2.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((cycleMillis * 4.1).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_morph2"
    )
    val morphedColors2 = altColors.mapIndexed { i, c ->
        val target = altColors[altColors.size - 1 - i]
        lerp(c, target, morphProgress2)
    }
    val brush2 = Brush.linearGradient(
        colors = morphedColors2,
        start = Offset(x = margin + offset2 + parallax.x, y = margin + offset2/2 + parallax.y),
        end = Offset(x = bandWidth + margin + offset2 + parallax.x, y = bandWidth/2 + margin + offset2/2 + parallax.y)
    )
    return brush1 to brush2
}

@Composable
fun rememberSlidingBrush(
    colors: List<Color> = listOf(
        Color(0xFF4FACFE),   // cyan‑blue
        Color(0xFF00F2FE),   // aqua
        Color(0xFF9B42F2),   // purple
        Color(0xFF4FACFE)    // back to cyan so the ends meet
    ),
    cycleMillis: Int = 4000,
    windowSize: IntSize? = null
): Brush {
    // 1. Use the actual width of the visible window as bandWidthPx
    val bandWidthPx = windowSize?.width?.toFloat() ?: with(LocalDensity.current) { 1000.dp.toPx() }
    // 2. Duplicate the first color at the end
    val seamlessColors = if (colors.size > 1 && colors.first() != colors.last()) colors + colors.first() else colors
    // 3. Animate offset from 0 to bandWidthPx
    val offset by rememberInfiniteTransition(label = "gradientShift")
        .animateFloat(
            initialValue = 0f,
            targetValue  = bandWidthPx,
            animationSpec = infiniteRepeatable(
                animation = tween(cycleMillis, easing = LinearEasing)
            ),
            label = "shiftX"
        )
    // 4. Draw the gradient from Offset(offset, 0f) to Offset(offset + bandWidthPx, 0f)
    return remember(offset, bandWidthPx, seamlessColors) {
        Brush.linearGradient(
            colors = seamlessColors,
            start = Offset(offset, 0f),
            end = Offset(offset + bandWidthPx, 0f)
        )
    }
}
