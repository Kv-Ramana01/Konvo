package com.example.konvo.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.konvo.navigation.Dest


@Composable
fun OnboardScreen(nav: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👋 Welcome, first‑timer!")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { nav.navigate(Dest.CHATLIST){
            popUpTo(0)
            launchSingleTop = true
        } }) {
            Text("Continue")
        }
    }
}
