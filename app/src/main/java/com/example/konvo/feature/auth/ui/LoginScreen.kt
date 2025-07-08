package com.example.konvo.feature.auth.ui

import KonvoLogo

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.example.konvo.ui.theme.KonvoNavyLight
import com.example.konvo.ui.theme.KonvoOrangeDark
import com.example.konvo.ui.util.AnimatedGradient
import com.example.konvo.util.rememberKeyboardHider
import com.example.konvo.util.rememberSlidingBrush
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    nav: NavController,
    vm: AuthViewModel = hiltViewModel()
) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val ctx = LocalContext.current

    val snackbarHost = remember { SnackbarHostState() }
    val hideKeyboard = rememberKeyboardHider()
    // Hoist the rememberSlidingBrush() call here
    val slidingBrush = AnimatedGradient()


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

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor  = Color.White,
        cursorColor          = Color.White,
        focusedBorderColor   = Color.White,
        unfocusedBorderColor = KonvoBlue.copy(.4f),
        focusedLeadingIconColor = Color.White,
        unfocusedLeadingIconColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        focusedPlaceholderColor = Color.White.copy(.3f),
        unfocusedPlaceholderColor     = Color.White.copy(.3f)
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


                Image(
                    painter = painterResource(R.drawable.logo_konvo_white),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(500.dp)
                        .aspectRatio(4f)
                        .drawWithCache {

                            onDrawWithContent {
                                drawContext.canvas.saveLayer(size.toRect(), Paint())
                                drawContent()
                                drawRect(brush = slidingBrush, blendMode = BlendMode.SrcIn)
                                drawContext.canvas.restore()
                            }
                        }
                )

                val anima = AnimatedGradient()

                OutlinedTextField(
                    email, { email = it },
                    label = { 
                        Text(
                            "Email",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = AnimatedGradient(),
                                fontSize = 14.sp
                            )
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Email, 
                            null,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = KonvoBlue.copy(.4f),
                        focusedLeadingIconColor = Color.White,
                        unfocusedLeadingIconColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedPlaceholderColor = Color.White.copy(.3f),
                        unfocusedPlaceholderColor = Color.White.copy(.3f)
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        brush = AnimatedGradient(),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    password, { password = it },
                    label = { 
                        Text(
                            "Password",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = AnimatedGradient(),
                                fontSize = 14.sp
                            )
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Lock, 
                            null,
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
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = KonvoBlue.copy(.4f),
                        focusedLeadingIconColor = Color.White,
                        unfocusedLeadingIconColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedPlaceholderColor = Color.White.copy(.3f),
                        unfocusedPlaceholderColor = Color.White.copy(.3f)
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        brush = AnimatedGradient(),
                        fontSize = 16.sp
                    ),
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
                    Text(
                        buildAnnotatedString {
                            append("New? ")
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append("Create account")
                            }
                        },
                        style = TextStyle(
                            brush = AnimatedGradient(),
                            fontSize = 15.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.35f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        )
                    )
                }


                Spacer(Modifier.height(24.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.White.copy(0.25f))
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { vm.launchGoogle(ctx, googleLauncher) },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
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
                    Text("Continue with Google", color = Color.Black)
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
