/*  KonvoNavGraph.kt  */
package com.example.konvo.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.konvo.feature.auth.ui.*
import com.example.konvo.feature.auth.vm.AuthViewModel
import com.example.konvo.feature.chatlist.ui.ChatListScreen
import com.example.konvo.ui.theme.transition.*
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

object Dest {
    const val SPLASH   = "splash"
    const val LOGIN    = "login"
    const val SIGNUP   = "signup"
    const val AUTH     = "auth"
    const val ONBOARD  = "onboard"
    const val PHONE    = "phone"
    const val OTP      = "otp"
    const val CHATLIST = "chatlist"
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KonvoNavGraph() {

    val nav = rememberNavController()

    val backdrop = Brush.verticalGradient(
        listOf(Color(0xFF0F144A), Color(0xFF5726A6))
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(backdrop)
    ) {

        AnimatedNavHost(
            navController    = nav,
            startDestination = Dest.SPLASH
        ) {
            /* ---------- Splash ---------- */
            composable(
                Dest.SPLASH,
                enterTransition = {
                    fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 1.08f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 1.08f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                }
            ) { SplashScreen(nav) }

            /* ========== AUTH FLOW ========== */
            navigation(
                route            = Dest.AUTH,
                startDestination = Dest.LOGIN
            ) {

                /* ---------- Login ---------- */
                composable(
                    Dest.LOGIN,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.96f)
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f)
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.04f)
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.96f)
                    }
                ) { LoginScreen(nav) }

                /* ---------- Signup ----------- */
                composable(
                    Dest.SIGNUP,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.96f)
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f)
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.04f)
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.96f)
                    }
                ) { backStackEntry ->
                    val parent = remember(backStackEntry) { nav.getBackStackEntry(Dest.AUTH) }
                    val vm: AuthViewModel = hiltViewModel(parent)
                    SignupScreen(nav, vm)
                }

                /* ---------- Phone ---------- */
                composable(
                    Dest.PHONE,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.96f)
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f)
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.04f)
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.96f)
                    }
                ) { backStackEntry ->
                    val parent = remember(backStackEntry) {
                        nav.getBackStackEntry(Dest.AUTH)
                    }
                    val vm: AuthViewModel = hiltViewModel(parent)
                    PhoneLoginScreen(nav, vm)
                }

                /* ---------- OTP ---------- */
                composable(
                    Dest.OTP,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.96f)
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f)
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f)
                    }
                ) { backStackEntry ->
                    val parent = remember(backStackEntry) {
                        nav.getBackStackEntry(Dest.AUTH)
                    }
                    val vm: AuthViewModel = hiltViewModel(parent)
                    OtpScreen(nav, vm)
                }
            }
            /* ================================= */

            /* ---------- Chat list ---------- */
            composable(
                Dest.CHATLIST,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.92f)
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.08f)
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.08f)
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.92f)
                }
            ) { ChatListScreen(nav) }

            /* ---------- On‑boarding ---------- */
            composable(
                Dest.ONBOARD,
                enterTransition = {
                    fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 1.08f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                }
            ) { OnboardScreen(nav) }
        }
    }
}
