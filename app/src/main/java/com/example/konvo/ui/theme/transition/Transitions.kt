package com.example.konvo.ui.theme.transition

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

private const val DUR = 300

/* ---------- simple fades ---------- */
fun fadeIn()  = fadeIn(animationSpec = tween(DUR))
fun fadeOut() = fadeOut(animationSpec = tween(DUR))

/* ---------- horizontal “push” / “parallax pop” ---------- */
fun slidePushIn(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it / 2 },    // from right half‑way
        animationSpec  = tween(DUR)
    ) + fadeIn()

fun slidePushOut(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -it / 2 },    // to left half‑way
        animationSpec = tween(DUR)
    ) + fadeOut()

fun parallaxPopIn(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -it / 2 },   // from left half‑way
        animationSpec  = tween(DUR)
    ) + fadeIn()

fun parallaxPopOut(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it / 2 },     // to right half‑way
        animationSpec = tween(DUR)
    ) + fadeOut()

/* ---------- zoom fade (OTP sheet style) ---------- */
fun zoomFadeIn(): EnterTransition =
    scaleIn(
        initialScale = 0.9f,
        animationSpec = tween(DUR)
    ) + fadeIn()

fun zoomFadeOut(): ExitTransition =
    scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(DUR)
    ) + fadeOut()
