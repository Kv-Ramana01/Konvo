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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    nav: NavController,
    vm: AuthViewModel
) {
    var phone by remember { mutableStateOf("") }
    val ctx = LocalActivity.current

    val snackbarHost = remember { SnackbarHostState() }
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
        OutlinedTextField(
            phone, { phone = it },
            label = { Text("Phone (+91551234567)") },
            singleLine = true,
            colors = tfColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {ctx?.let { vm.trySendOrResend(phone.trim(), it) }
            },
            enabled = phone.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Send OTP") }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { nav.popBackStack() }) { Text("Back") }
    }
}
