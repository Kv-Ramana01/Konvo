/*  ui/util/AnimatedGradient.kt  */
package com.example.konvo.ui.util

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun AnimatedGradient(
    /** long seamless band – duplicate the first colour at the end for a wrap‑around */
    colors: List<Color> = listOf(
        Color(0xFF4FACFE),   // cyan‑blue
        Color(0xFF00F2FE),   // aqua
        Color(0xFF9B42F2),   // purple
        Color(0xFF4FACFE)    // back to cyan so the ends meet
    ),
    cycleMillis: Int = 4000           // total time for one full pass
): Brush {


    val bandWidthPx = with(LocalDensity.current) { 1000.dp.toPx() }


    val offset by rememberInfiniteTransition(label = "gradientShift")
        .animateFloat(
            initialValue = 0f,
            targetValue  = bandWidthPx,
            animationSpec = infiniteRepeatable(
                animation = tween(cycleMillis, easing = LinearEasing)
            ),
            label = "shiftX"
        )


    return remember(offset) {
        Brush.linearGradient(
            colors       = colors,
            start        = Offset(-bandWidthPx + offset, 0f),
            end          = Offset(offset,                0f)
        )
    }
}