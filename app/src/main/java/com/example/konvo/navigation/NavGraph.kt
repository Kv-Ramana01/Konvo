/*  KonvoNavGraph.kt  */
package com.example.konvo.navigation

import androidx.compose.animation.*
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
                exitTransition     = { fadeOut() },
                popEnterTransition = { fadeIn() }
            ) { SplashScreen(nav) }

            /* ========== AUTH FLOW ========== */
            navigation(
                route            = Dest.AUTH,
                startDestination = Dest.LOGIN
            ) {

                /* ---------- Login ---------- */
                composable(
                    Dest.LOGIN,
                    enterTransition   = { fadeIn() },
                    exitTransition    = { slidePushOut() },
                    popEnterTransition= { parallaxPopIn() },
                    popExitTransition = { fadeOut() }
                ) { LoginScreen(nav) }

                /* ---------- Signup ----------- */
                composable(
                    Dest.SIGNUP,
                    enterTransition     = { slidePushIn() },
                    exitTransition      = { slidePushOut() },
                    popEnterTransition  = { parallaxPopIn() },
                    popExitTransition   = { parallaxPopOut() }
                ) { backStackEntry ->
                    val parent = remember(backStackEntry) { nav.getBackStackEntry(Dest.AUTH) }
                    val vm: AuthViewModel = hiltViewModel(parent)
                    SignupScreen(nav, vm)
                }

                /* ---------- Phone ---------- */
                composable(
                    Dest.PHONE,
                    enterTransition   = { slidePushIn() },
                    exitTransition    = { slidePushOut() },
                    popEnterTransition= { parallaxPopIn() },
                    popExitTransition = { parallaxPopOut() }
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
                    enterTransition = { zoomFadeIn() },
                    /*  special: when we go forward to CHATLIST
                        we slide/fade OTP away; otherwise default  */
                    exitTransition = {
                        if (targetState.destination.route == Dest.CHATLIST) {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 2 },
                                animationSpec = tween(300)
                            ) + fadeOut(tween(250))
                        } else {
                            zoomFadeOut()
                        }
                    },
                    popExitTransition = { zoomFadeOut() }
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
                    if (initialState.destination.route == Dest.OTP) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350)
                        ) + fadeIn(tween(300))
                    } else {
                        fadeIn()
                    }
                },
                popExitTransition = { fadeOut() }
            ) { ChatListScreen(nav) }

            /* ---------- On‑boarding ---------- */
            composable(
                Dest.ONBOARD,
                enterTransition = { zoomFadeIn() },
                popExitTransition = { zoomFadeOut() }
            ) { OnboardScreen(nav) }
        }
    }
}
