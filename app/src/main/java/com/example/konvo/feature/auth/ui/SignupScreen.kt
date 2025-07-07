package com.example.konvo.feature.auth.ui

import androidx.activity.result.launch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.konvo.feature.auth.vm.AuthEvent
import com.example.konvo.feature.auth.vm.AuthViewModel
import com.example.konvo.navigation.Dest
import androidx.compose.runtime.rememberCoroutineScope
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.util.rememberKeyboardHider
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    nav: NavController,
    vm: AuthViewModel = hiltViewModel()
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    hideKeyboard()
                    awaitFrame()
                    nav.navigate(Dest.ONBOARD) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }

                is AuthEvent.Error -> snackbarHost.showSnackbar(event.msg)
                else -> Unit
            }
        }
    }

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF001E3C), Color(0xFF283593), Color(0xFF512DA8))
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    colors = tfColors,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = tfColors,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            vm.registerWithEmail(email, password)
                        } else {

                            scope.launch {
                                snackbarHost.showSnackbar("Please fill both fields")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Account")
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { nav.popBackStack() }) {
                    Text("Back to Login")
                }
            }
        }
    }
}
