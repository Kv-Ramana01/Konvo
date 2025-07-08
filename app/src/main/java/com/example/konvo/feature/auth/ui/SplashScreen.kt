package com.example.konvo.feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.example.konvo.R
import com.example.konvo.navigation.Dest
import com.example.konvo.ui.util.AnimatedGradient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.time.format.TextStyle

/* Gradient title */
@Composable
fun GradientText(word: String) = Text(
    text = word,
    style = androidx.compose.ui.text.TextStyle(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF4FACFE), Color(0xFF9B42F2))
        ),
        fontSize = 46.sp,
        fontWeight = FontWeight.ExtraBold
    )
)

/* Type‑writer with blinking cursor */
@Composable
fun TypewriterText(full: String) {
    var count by remember { mutableIntStateOf(0) }
    val blink by rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "blink"
    )
    LaunchedEffect(Unit) {
        full.forEachIndexed { i, _ -> delay(45); count = i + 1 }
    }
    Row {
        Text(full.take(count), color = Color.White, fontSize = 16.sp)
        if (count < full.length)
            Text("_", color = Color.White.copy(alpha = blink), fontSize = 16.sp)
    }
}

/* Three pulsing dots (Compose only) */
@Composable
fun TypingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    val scale1 by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
    val scale2 by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse))
    val scale3 by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Dot(scale1); Dot(scale2); Dot(scale3)
    }
}

@Composable
fun Dot(scale: Float) = Box(
    Modifier
        .size(14.dp)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .background(Color.White, CircleShape)
)

@Composable
fun SplashScreen(nav: NavController) {
    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF001E3C),  // navy (top)
            Color(0xFF283593),  // indigo (mid)
            Color(0xFF512DA8)   // deep purple (bottom)
        ),
        startY = 0f,
        endY   = Float.POSITIVE_INFINITY     // fill the whole height
    )

    /* ---------- animated background drift ---------- */
    val drift = rememberInfiniteTransition(label = "bg")
    val top by drift.animateColor(
        initialValue = Color(0xFF0F144A),
        targetValue  = Color(0xFF162676),
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "topCol"
    )
    val bottom by drift.animateColor(
        initialValue = Color(0xFF5726A6),
        targetValue  = Color(0xFF4A218F),
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "botCol"
    )

    /* ---------- staged entrance control ---------- */
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200); show = true           // start intro
        delay(2_800)                      // total splash time
        val start = if (FirebaseAuth.getInstance().currentUser == null)
            Dest.AUTH          // shows Login flow
        else Dest.CHATLIST     // jumps straight in
        nav.navigate(start) {
            popUpTo(Dest.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    val slidingBrush = AnimatedGradient()

    Box(
        Modifier
            .fillMaxSize()
            .background(gradientBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            /* Konvo title */
            AnimatedVisibility(
                show,
                enter = slideInVertically(
                    initialOffsetY = { -120 },
                    animationSpec = tween(600, easing = EaseOutCubic)
                ) + fadeIn(tween(600))
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_konvo_white),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(500.dp)
                        .aspectRatio(4f)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContext.canvas.saveLayer(size.toRect(), Paint())
                                drawContent()
                                drawRect(brush = slidingBrush, blendMode = BlendMode.SrcIn)
                                drawContext.canvas.restore()
                            }
                        }
                )
            }

            /* Divider */
            AnimatedVisibility(
                show,
                enter = expandHorizontally(
                    animationSpec = tween(500, 200, EaseOutCubic),
                    expandFrom = Alignment.Start
                )
            ) {
                Box(
                    Modifier
                        .height(2.dp)
                        .width(160.dp)
                        .background(Color.White.copy(0.8f), RoundedCornerShape(1.dp))
                )
            }

            /* Tagline with type‑writer cursor */
            AnimatedVisibility(
                show,
                enter = fadeIn(tween(500, 300))
            ) {
                TypewriterText("Start your konvo now!")
            }

            Spacer(Modifier.height(32.dp))



            Spacer(Modifier.height(16.dp))

            /* Three‑dot typing indicator */
            AnimatedVisibility(
                show,
                enter = scaleIn(tween(600, 350), initialScale = 0.8f)
            ) {
                TypingDots()
            }
        }
    }
}

