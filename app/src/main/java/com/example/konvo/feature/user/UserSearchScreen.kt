package com.example.konvo.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.example.konvo.data.FirestoreRepository

// Data class for user search result
 data class UserProfile(
    val uid: String,
    val name: String,
    val username: String
)

// Move this to a top-level utility so it can be imported everywhere
fun get1to1ChatId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "${'$'}uid1_${'$'}uid2" else "${'$'}uid2_${'$'}uid1"
}

suspend fun ensureChatlistEntry(myUid: String, other: UserProfile): Pair<String, String> {
    val chatId = FirestoreRepository.getChatId(myUid, other.uid)
    
    try {
        // Get current user's information
        val currentUserDoc = FirebaseFirestore.getInstance().collection("users").document(myUid).get().await()
        val myName = currentUserDoc.getString("name") ?: "You"
        val myUsername = currentUserDoc.getString("username") ?: myName
        
        // Create or update chatlist entry for current user
        val myData = hashMapOf(
            "lastMessage" to "",
            "timestamp" to com.google.firebase.Timestamp.now(),
            "lastMessageTime" to System.currentTimeMillis().toString(),
            "otherUserId" to other.uid,
            "userName" to other.name,
            "userUsername" to other.username,
            "unreadCount" to 0
        )
        
        // Create or update chatlist entry for other user
        val otherData = hashMapOf(
            "lastMessage" to "",
            "timestamp" to com.google.firebase.Timestamp.now(), 
            "lastMessageTime" to System.currentTimeMillis().toString(),
            "otherUserId" to myUid,
            "userName" to myName,
            "userUsername" to myUsername,
            "unreadCount" to 0
        )
        
        // Use a batch to ensure both entries are created atomically
        val batch = FirebaseFirestore.getInstance().batch()
        
        val myRef = FirebaseFirestore.getInstance().collection("users")
            .document(myUid).collection("chats").document(chatId)
            
        val otherRef = FirebaseFirestore.getInstance().collection("users")
            .document(other.uid).collection("chats").document(chatId)
            
        batch.set(myRef, myData, com.google.firebase.firestore.SetOptions.merge())
        batch.set(otherRef, otherData, com.google.firebase.firestore.SetOptions.merge())
        
        batch.commit().await()
        
    } catch (e: Exception) {
        println("[UserSearchScreen] Error ensuring chat entries: ${e.message}")
        // Continue with the function, as we still want to return the chatId
    }
    
    return Pair(chatId, other.username)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<UserProfile>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val myUid = currentUser?.uid
    val myName = currentUser?.displayName ?: "You"

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            error = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val db = FirebaseFirestore.getInstance()
            val usersByName = db.collection("users")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + '\uf8ff')
                .get().await().documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val username = doc.getString("username") ?: return@mapNotNull null
                    UserProfile(doc.id, name, username)
                }
            val usersByUsername = db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + '\uf8ff')
                .get().await().documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val username = doc.getString("username") ?: return@mapNotNull null
                    UserProfile(doc.id, name, username)
                }
            // Merge and deduplicate
            val all = (usersByName + usersByUsername).distinctBy { it.uid }
            results = all
        } catch (e: Exception) {
            error = e.localizedMessage
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search users by name or username...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color.Red)
                }
            } else if (results.isEmpty() && query.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found.", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results) { user ->
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("@${user.username}", color = Color.Gray, fontSize = 14.sp)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val (chatId, displayName) = ensureChatlistEntry(myUid!!, user)
                                            nav.navigate("chat/${myUid}/${user.uid}/${user.name}/false")
                                        }
                                    },
                                    enabled = myUid != null && user.uid != myUid,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Add to Chatlist")
                                }
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = myUid != null && user.uid != myUid) {
                                        scope.launch {
                                            val (chatId, displayName) = ensureChatlistEntry(myUid!!, user)
                                            nav.navigate("chat/${myUid}/${user.uid}/${user.name}/false")
                                        }
                                    }
                                    .padding(start = 32.dp, end = 16.dp, bottom = 8.dp, top = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Start Chat", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
} 