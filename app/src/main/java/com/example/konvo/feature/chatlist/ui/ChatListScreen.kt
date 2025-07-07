package com.example.konvo.feature.chatlist.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.konvo.feature.auth.vm.signOutEverywhere
import com.example.konvo.navigation.Dest
import com.example.konvo.util.findActivity
import com.google.api.Context
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(nav: NavController) {
    val ctx = LocalContext.current

    val act = LocalContext.current.findActivity()
    BackHandler { act?.finish() }

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚡ Dummy Chat List ⚡", fontSize = 24.sp)
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    signOutEverywhere(ctx) {
                        nav.navigate(Dest.LOGIN) { popUpTo(0) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }
    }
}
