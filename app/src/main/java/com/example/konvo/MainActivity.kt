package com.example.konvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.konvo.navigation.KonvoNavGraph
import com.example.konvo.ui.theme.KonvoTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.konvo.data.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Activity-level coroutine scope
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KonvoTheme {
                KonvoNavGraph()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        updateUserOnlineStatus(true)
    }
    
    override fun onStop() {
        super.onStop()
        updateUserOnlineStatus(false)
    }
    
    private fun updateUserOnlineStatus(isOnline: Boolean) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        
        activityScope.launch {
            try {
                FirestoreRepository.updateUserOnlineStatus(currentUserId, isOnline)
            } catch (e: Exception) {
                println("[MainActivity] Error updating online status: ${e.message}")
            }
        }
    }
}
