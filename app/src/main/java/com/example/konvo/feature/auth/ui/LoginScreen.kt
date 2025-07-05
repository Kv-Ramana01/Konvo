package com.example.konvo.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.ui.theme.KonvoNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF001E3C),  // navy (top)
            Color(0xFF283593),  // indigo (mid)
            Color(0xFF512DA8)   // deep purple (bottom)
        ),
        startY = 0f,
        endY   = Float.POSITIVE_INFINITY     // fill the whole height
    )

    /* Gradient for background & heading */
    val konvoGradient = Brush.verticalGradient(
        colors = listOf(KonvoBlue, KonvoBlueDark),
        startY = 0f,
        endY = 800f        // fade out by mid‑screen
    )

    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        //textColor          = Color.White,
        cursorColor        = Color.White,
        focusedBorderColor = KonvoBlueDark,
        unfocusedBorderColor = KonvoBlue.copy(alpha = 0.6f),
        focusedLabelColor  = KonvoBlue,
        unfocusedLabelColor= KonvoBlueDark.copy(alpha = 0.7f)
    )


    /* Root container with gradient bg */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            /* Fancy Konvo heading */
            Text(
                text = "Konvo",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp),
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = konvoGradient,
                    textAlign = TextAlign.Center
                )
            )

            /* === Email field === */
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )

            Spacer(Modifier.height(12.dp))

            /* === Password field === */
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )

            Spacer(Modifier.height(24.dp))

            /* Primary login button */
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Login")
            }

            /* Secondary action */
            TextButton(onClick = onRegisterClick) {
                Text("Create new account")
            }
        }
    }
}
