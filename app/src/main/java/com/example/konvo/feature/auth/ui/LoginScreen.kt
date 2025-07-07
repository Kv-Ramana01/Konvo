package com.example.konvo.feature.auth.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.konvo.R
import com.example.konvo.feature.auth.vm.AuthEvent
import com.example.konvo.feature.auth.vm.AuthViewModel
import com.example.konvo.navigation.Dest
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.util.rememberKeyboardHider
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    nav: NavController,
    vm: AuthViewModel = hiltViewModel()
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    val snackbarHost = remember { SnackbarHostState() }
    val hideKeyboard = rememberKeyboardHider()


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

                AuthEvent.NewUser -> nav.navigate(Dest.ONBOARD) {
                    popUpTo(0)
                    launchSingleTop = true
                }

                is AuthEvent.Error -> snackbarHost.showSnackbar(event.msg)

                else -> Unit
            }
        }
    }

    val googleLauncher =
        rememberLauncherForActivityResult(StartActivityForResult()) { res ->
            vm.handleGoogleResult(res)
        }

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF001E3C), Color(0xFF283593), Color(0xFF512DA8))
    )
    val headingGradient = Brush.verticalGradient(listOf(KonvoBlue, KonvoBlueDark))
    val tfColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedTextColor = Color.White,
        unfocusedTextColor = KonvoBlue,
        cursorColor = Color.White,
        focusedBorderColor = KonvoBlueDark,
        unfocusedBorderColor = KonvoBlue.copy(alpha = 0.6f),
        focusedLabelColor = KonvoBlue,
        unfocusedLabelColor = KonvoBlueDark.copy(alpha = 0.7f)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    "Konvo",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = headingGradient,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    email, { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    colors = tfColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    password, { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = tfColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        vm.loginWithEmail(email.trim(), password) },
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Login") }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { nav.navigate(Dest.SIGNUP) }) {
                    Text("New?  Create account")
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White.copy(0.25f))
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { vm.launchGoogle(ctx, googleLauncher) },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.glg1),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Continue with Google")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { nav.navigate(Dest.PHONE) },
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with phone")
                }
            }
        }
    }
}
