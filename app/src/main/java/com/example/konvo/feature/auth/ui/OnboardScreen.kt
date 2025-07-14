package com.example.konvo.feature.auth.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun OnboardScreen(nav: NavController) {
    val user = Firebase.auth.currentUser
    val uid = user?.uid
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    fun checkUsernameAvailability(username: String) {
        if (username.isBlank()) {
            usernameAvailable = null
            return
        }
        loading = true
        error = ""
        scope.launch {
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("users").whereEqualTo("username", username).get().await()
            usernameAvailable = query.isEmpty
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome to Konvo!", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    "Let's set up your profile so friends can find you.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4FACFE),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameAvailable = null
                        if (it.length >= 3) checkUsernameAvailability(it)
                    },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4FACFE),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    supportingText = {
                        Text("Must be unique, at least 3 characters", fontSize = 12.sp, color = Color.Gray)
                    }
                )
                if (loading && username.isNotBlank() && username.length >= 3) {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Checking username...", fontSize = 13.sp, color = Color.Gray)
                    }
                } else if (username.isNotBlank() && username.length >= 3) {
                    when (usernameAvailable) {
                        true -> Text("Username available", color = Color(0xFF4CAF50), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        false -> Text("Username taken", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        null -> {}
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (uid == null) {
                            error = "User not signed in."
                            scope.launch { snackbarHostState.showSnackbar(error) }
                            return@Button
                        }
                        if (name.isBlank() || username.isBlank() || usernameAvailable != true) {
                            error = "Please enter a valid name and available username."
                            scope.launch { snackbarHostState.showSnackbar(error) }
                            return@Button
                        }
                        loading = true
                        error = ""
                        scope.launch {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(uid).set(
                                mapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "username" to username,
                                    "createdAt" to System.currentTimeMillis()
                                )
                            ).addOnSuccessListener {
                                nav.navigate(Dest.CHATLIST) {
                                    popUpTo(0)
                                    launchSingleTop = true
                                }
                            }.addOnFailureListener {
                                error = "Failed to save profile: ${it.message}"
                                loading = false
                                scope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && username.isNotBlank() && usernameAvailable == true && !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Next", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}
