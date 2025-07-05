package com.example.konvo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.konvo.feature.auth.ui.LoginScreen

object Dest {
    const val LOGIN   = "login"
    const val CHATLIST = "chatlist"
    const val CHAT     = "chat/{chatId}"
}

@Composable
fun KonvoNavGraph(startDest: String = Dest.LOGIN) {
    val nav = rememberNavController()
    NavHost(nav, startDest) {
        composable(Dest.LOGIN) {
            LoginScreen(
                onLoginClick    = { nav.navigate(Dest.CHATLIST) },   // temporary mock
                onRegisterClick = { /* TODO: navigate to register */ }
            )
        }
/*
        composable(Dest.CHATLIST){ ChatListScreen(nav) }
        composable(
            Dest.CHAT,
            arguments = listOf(navArgument("chatId"){ type = NavType.StringType })
        ) { back ->
            ChatScreen(chatId = back.arguments!!.getString("chatId")!!)
        }

 */
    }
}
