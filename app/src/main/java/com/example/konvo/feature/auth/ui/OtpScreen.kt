package com.example.konvo.feature.auth.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.konvo.feature.auth.vm.AuthEvent
import com.example.konvo.feature.auth.vm.AuthViewModel
import com.example.konvo.navigation.Dest
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.util.rememberKeyboardHider
import kotlinx.coroutines.android.awaitFrame


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    nav: NavController,
    vm : AuthViewModel
) {
    var code by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    val hideKeyboard = rememberKeyboardHider()

    val tfColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedTextColor = Color.White,
        unfocusedTextColor = KonvoBlue,
        cursorColor = Color.White,
        focusedBorderColor = KonvoBlueDark,
        unfocusedBorderColor = KonvoBlue.copy(alpha = 0.6f),
        focusedLabelColor = KonvoBlue,
        unfocusedLabelColor = KonvoBlueDark.copy(alpha = 0.7f)
    )


    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                AuthEvent.Success -> {
                    hideKeyboard()
                    awaitFrame()
                    nav.navigate(Dest.CHATLIST) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }

                AuthEvent.NewUser -> {
                    nav.navigate(Dest.ONBOARD) {                    // first‑time flow
                        popUpTo(Dest.SPLASH) { inclusive = true }
                    }
                }

                AuthEvent.OtpSent -> {
                    nav.navigate(Dest.OTP)
                }

                is AuthEvent.Error ->  Toast.makeText(ctx, event.msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    /* ---- UI ---- */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        OutlinedTextField(
            value         = code,
            onValueChange = { if (it.length <= 6) code = it },
            label         = { Text("Enter 6‑digit code") },
            singleLine    = true,
            colors = tfColors,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick  = { vm.verifyOtp(code) },
            enabled  = code.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Verify") }

        Spacer(Modifier.height(12.dp))

        TextButton(
            enabled = vm.lastPhone != null && act != null,
            onClick = { act?.let(vm::resendLastOtp) }
        ) { Text("Resend code") }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { nav.popBackStack() }) { Text("Back") }
    }
}
