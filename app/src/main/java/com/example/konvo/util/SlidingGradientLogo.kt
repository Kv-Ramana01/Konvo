package com.example.konvo.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.*
import androidx.compose.runtime.setValue
import com.example.konvo.R
import com.example.konvo.ui.util.AnimatedGradient
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill

@Composable
fun KonvoLogo(
    modifier: Modifier = Modifier,
    cycleMs: Int = 10000
) {
    val brush = AnimatedGradient()
    Image(
        painter = painterResource(R.drawable.logo_konvo_white),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(260.dp)
            .aspectRatio(4f)
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
            }
    )
}

@Composable
fun LoopingSlidingGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    speedMillis: Int = 4000,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize(0, 0)) }
    val bandWidth = size.width.toFloat().takeIf { it > 0 } ?: 1f
    val infiniteTransition = rememberInfiniteTransition(label = "slide")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = bandWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMillis, easing = LinearEasing)
        ),
        label = "slideOffset"
    )
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = if (colors.first() != colors.last()) colors + colors.first() else colors,
                    start = Offset.Zero,
                    end = Offset(bandWidth, 0f)
                )
                onDrawBehind {
                    withTransform({ translate(left = offset) }) {
                        drawRect(brush = brush, size = Size(bandWidth, size.height.toFloat()))
                    }
                    withTransform({ translate(left = offset - bandWidth) }) {
                        drawRect(brush = brush, size = Size(bandWidth, size.height.toFloat()))
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun DiagonalLoopingSlidingGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    speedMillis: Int = 4000,
    angleDegrees: Float = 30f, // New: custom angle
    bandLengthRatio: Float = 1.2f, // New: band length relative to width
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize(0, 0)) }
    val width = size.width.toFloat().takeIf { it > 0 } ?: 1f
    val height = size.height.toFloat().takeIf { it > 0 } ?: 1f
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val bandLength = (width * bandLengthRatio).toFloat()
    val dx = (bandLength * Math.cos(angleRad)).toFloat()
    val dy = (bandLength * Math.sin(angleRad)).toFloat()
    val infiniteTransition = rememberInfiniteTransition(label = "diagonalSlide")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = bandLength,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMillis, easing = LinearEasing)
        ),
        label = "diagonalSlideOffset"
    )
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = if (colors.first() != colors.last()) colors + colors.first() else colors,
                    start = Offset(0f, 0f),
                    end = Offset(dx, dy)
                )
                onDrawBehind {
                    withTransform({ translate(left = offset, top = offset * dy / dx) }) {
                        drawRect(brush = brush, size = Size(dx, dy))
                    }
                    withTransform({ translate(left = offset - bandLength, top = (offset - bandLength) * dy / dx) }) {
                        drawRect(brush = brush, size = Size(dx, dy))
                    }
                }
            }
    ) {
        content()
    }
}
