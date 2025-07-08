package com.example.konvo.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.konvo.R
import com.example.konvo.ui.util.AnimatedGradient
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    nav: NavController,
    vm: AuthViewModel = hiltViewModel()
) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hideKeyboard = rememberKeyboardHider()


    val tfColors = OutlinedTextFieldDefaults.colors(
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

    val anima = AnimatedGradient()

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
                // Logo with animated gradient
                Image(
                    painter = painterResource(R.drawable.logo_konvo_white),
                    contentDescription = null,
                    modifier = Modifier
                        .width(320.dp)
                        .aspectRatio(4f)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContext.canvas.saveLayer(size.toRect(), Paint())
                                drawContent()
                                drawRect(brush = anima, blendMode = BlendMode.SrcIn)
                                drawContext.canvas.restore()
                            }
                        }
                )
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            "Email",
                            style = TextStyle(
                                brush = anima,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = KonvoBlue.copy(alpha = 0.6f),
                        focusedLeadingIconColor = Color.White,
                        unfocusedLeadingIconColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = KonvoBlueDark.copy(alpha = 0.7f)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.drawWithCache {
                                onDrawWithContent {
                                    drawContext.canvas.saveLayer(size.toRect(), Paint())
                                    drawContent()
                                    drawRect(brush = anima, blendMode = BlendMode.SrcIn)
                                    drawContext.canvas.restore()
                                }
                            }
                        )
                    },
                    textStyle = TextStyle(
                        brush = anima,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            "Password",
                            style = TextStyle(
                                brush = anima,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = KonvoBlue.copy(alpha = 0.6f),
                        focusedLeadingIconColor = Color.White,
                        unfocusedLeadingIconColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = KonvoBlueDark.copy(alpha = 0.7f)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.drawWithCache {
                                onDrawWithContent {
                                    drawContext.canvas.saveLayer(size.toRect(), Paint())
                                    drawContent()
                                    drawRect(brush = anima, blendMode = BlendMode.SrcIn)
                                    drawContext.canvas.restore()
                                }
                            }
                        )
                    },
                    textStyle = TextStyle(
                        brush = anima,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KonvoBlueDark,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, KonvoBlueDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = KonvoBlueDark
                    )
                ) {
                    Text("Back to Login", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
