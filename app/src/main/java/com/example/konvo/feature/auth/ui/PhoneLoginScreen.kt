package com.example.konvo.feature.auth.ui
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PhoneLoginScreen(
    nav: NavController,
    vm: AuthViewModel
) {
    var phone by rememberSaveable { mutableStateOf("") }
    val ctx = LocalActivity.current

    val snackbarHost = remember { SnackbarHostState() }
    val hideKeyboard = rememberKeyboardHider()

    val anima = AnimatedGradient()

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


                AuthEvent.OtpSent -> {
                    hideKeyboard()
                    awaitFrame()
                    nav.navigate(Dest.OTP)
                }


                AuthEvent.Success -> {

                    nav.navigate(Dest.CHATLIST) {

                        popUpTo(0)
                        launchSingleTop = true
                    }
                }


                AuthEvent.NewUser -> nav.navigate(Dest.ONBOARD) {
                    popUpTo(Dest.SPLASH) { inclusive = true }
                }


                is AuthEvent.Error -> snackbarHost.showSnackbar(event.msg)
            }
        }
    }

    SnackbarHost(hostState = snackbarHost)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            phone, { phone = it },
            label = { Text("Phone (+91551234567)", style = TextStyle(brush = anima, fontSize = 14.sp, fontWeight = FontWeight.Medium)) },
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
                    painterResource(R.drawable.ic_launcher_foreground),
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
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {ctx?.let { vm.trySendOrResend(phone.trim(), it) }
            },
            enabled = phone.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KonvoBlueDark,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(6.dp)
        ) { Text("Send OTP", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
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
            Text("Back", fontWeight = FontWeight.Medium)
        }
    }
}
