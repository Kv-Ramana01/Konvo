package com.example.konvo.feature.chatlist.ui


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konvo.R
import com.example.konvo.ui.theme.KonvoTheme
import com.example.konvo.ui.theme.konvoThemes
import androidx.navigation.NavController
import com.example.konvo.feature.auth.vm.signOutEverywhere
import com.example.konvo.feature.settings.ui.ProfileSettingsScreen
import com.example.konvo.navigation.Dest
import com.example.konvo.util.findActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.konvo.ui.util.AnimatedGradient
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kotlin.random.Random
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Chat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.draw.scale
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.konvo.util.rememberSlidingBrush
import com.example.konvo.util.themeDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.IntSize
import com.example.konvo.util.LoopingSlidingGradientBackground
import com.example.konvo.util.DiagonalLoopingSlidingGradientBackground
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.AlternateEmail
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.konvo.data.FirestoreRepository
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.filled.Close

// DataStore setup
private val THEME_INDEX_KEY = intPreferencesKey("theme_index")
private val ANIMATED_THEME_KEY = booleanPreferencesKey("animated_theme_enabled")

suspend fun saveThemePrefs(context: Context, themeIndex: Int, animated: Boolean) {
    context.themeDataStore.edit { prefs ->
        prefs[THEME_INDEX_KEY] = themeIndex
        prefs[ANIMATED_THEME_KEY] = animated
    }
}

suspend fun readThemePrefs(context: Context): Pair<Int, Boolean> {
    val prefs = context.themeDataStore.data.first()
    val themeIndex = prefs[THEME_INDEX_KEY] ?: 0
    val animated = prefs[ANIMATED_THEME_KEY] ?: true
    return themeIndex to animated
}

// Mock data classes
sealed class ChatItem(open val id: String)
data class GroupChat(
    val groupName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unread: Int
) : ChatItem(groupName)
data class DMChat(
    override val id: String, // chatId
    val userName: String, // display name
    val userUsername: String, // username
    val lastMessage: String,
    val lastMessageTime: String,
    val lastMessageTimeNumeric: Long? = null, // Add numeric timestamp for reliable sorting
    val unread: Int,
    val otherUserId: String,
    val profileImage: String? = null // Add profileImage field
) : ChatItem(id)

// Remove local KonvoTheme data class and themes list, use konvoThemes instead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // State for theme and animation toggle, loaded from DataStore
    var themeIndex by rememberSaveable { mutableStateOf(0) }
    var animatedThemeEnabled by rememberSaveable { mutableStateOf(true) }
    // Load from DataStore on first launch
    LaunchedEffect(Unit) {
        val (savedTheme, savedAnimated) = readThemePrefs(context)
        themeIndex = savedTheme
        animatedThemeEnabled = savedAnimated
    }
    // Save to DataStore whenever themeIndex or animatedThemeEnabled changes
    LaunchedEffect(themeIndex, animatedThemeEnabled) {
        saveThemePrefs(context, themeIndex, animatedThemeEnabled)
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showProfileImagePreview by remember { mutableStateOf<String?>(null) }
    val theme = konvoThemes[themeIndex]
    var search by rememberSaveable { mutableStateOf("") }
    // Animated theme toggle state
    var showLogoutDialog by remember { mutableStateOf(false) }
    // Show profile settings directly
    var showProfileSettings by remember { mutableStateOf(false) }

    // If profile settings should be shown, show it instead of normal chat list
    if (showProfileSettings) {
        // Handle back button to return to chat list
        BackHandler {
            showProfileSettings = false
        }
        ProfileSettingsScreen(
            navController = nav,
            onBackPressed = { showProfileSettings = false }
        )
        return
    }
    
    // Real-time chat list updates
    val userId = Firebase.auth.currentUser?.uid
    var chats by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? by remember { mutableStateOf(null) }
    
    // User profile data
    var userName by remember { mutableStateOf("") }
    var userProfileImage by remember { mutableStateOf<String?>(null) }
    var userOnline by remember { mutableStateOf(true) }
    
    // Load user profile data
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                userName = userDoc.getString("name") ?: "User"
                userProfileImage = userDoc.getString("profileImage")
            } catch (e: Exception) {
                println("[ChatListScreen] Error loading user profile: ${e.message}")
            }
        }
    }
    
    DisposableEffect(userId) {
        // Clean up any existing listener
        listenerRegistration?.remove()
        
        // If user is logged in, set up real-time chat list listener
        if (userId != null) {
            listenerRegistration = FirestoreRepository.listenForChatList(
                userId = userId,
                onChatListUpdate = { chatDataList ->
                    // Convert the raw data to our ChatItem objects
                    val chatItems = chatDataList.mapNotNull { data ->
                        val chatId = data["id"] as? String ?: return@mapNotNull null
                        val userName = data["userName"] as? String ?: "Unknown"
                        val userUsername = data["userUsername"] as? String ?: userName
                        val lastMessage = data["lastMessage"] as? String ?: ""
                        val lastMessageTime = data["lastMessageTime"] as? String ?: ""
                        val lastMessageTimeNumeric = (data["lastMessageTimeNumeric"] as? Long) 
                            ?: (data["lastMessageTime"] as? String)?.toLongOrNull()
                            ?: System.currentTimeMillis() // Fallback to current time
                        val unread = (data["unreadCount"] as? Long)?.toInt() ?: 0
                        val otherUserId = data["otherUserId"] as? String ?: ""
                        val profileImage = data["profileImage"] as? String
                        
                        println("[ChatListScreen] Chat data: id=$chatId, lastMessage=$lastMessage, lastMessageTime=$lastMessageTime, lastMessageTimeNumeric=$lastMessageTimeNumeric")
                        
                        // Create a DMChat item (we're not handling GroupChat items yet)
                        DMChat(
                            id = chatId,
                            userName = userName, 
                            userUsername = userUsername,
                            lastMessage = lastMessage,
                            lastMessageTime = lastMessageTime,
                            lastMessageTimeNumeric = lastMessageTimeNumeric,
                            unread = unread,
                            otherUserId = otherUserId,
                            profileImage = profileImage
                        )
                    }
                    
                    // Update the chats state
                    chats = chatItems
                },
                onError = { error ->
                    println("[ChatListScreen] Error listening for chat list: ${error.message}")
                    // Could show a snackbar or other error UI here
                }
            )
        }
        
        // Clean up on dispose
        onDispose {
            listenerRegistration?.remove()
        }
    }
    
    // Sort chats by lastMessageTime descending (most recent first)
    val sortedChats = chats.sortedByDescending {
        when (it) {
            is DMChat -> it.lastMessageTimeNumeric ?: 0L
            is GroupChat -> it.lastMessageTime.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    // Filter chats by search
    val filteredChats = sortedChats.filter {
        search.isBlank() ||
                (it is GroupChat && it.groupName.contains(search, true)) ||
                (it is DMChat && it.userName.contains(search, true))
    }

    // Calculate user initials for fallback avatar
    val userInitials = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
    val userColor = Color(0xFF4FACFE)

    val gradientColors = when (theme.name) {
        "Galactic Aurora" -> listOf(
            Color(0xFF0B0033),
            Color(0xFF3700B3),
            Color(0xFF00FFB3),
            Color(0xFF00CFFF),
            Color(0xFFB388FF)
        )
        "Cosmic Sunset" -> listOf(
            Color(0xFF1A0A2D),
            Color(0xFF3D155F),
            Color(0xFFFF6F91),
            Color(0xFFFF9671),
            Color(0xFFFFC75F)
        )
        "Stellar Ice" -> listOf(
            Color(0xFF0F2027),
            Color(0xFF2C5364),
            Color(0xFF36D1C4),
            Color(0xFF5B86E5),
            Color(0xFFB2FEFA)
        )
        "Cyberpunk" -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFFFF2E63),
            Color(0xFF08D9D6),
            Color(0xFFFFC300)
        )
        "Nebula Dream" -> listOf(
            Color(0xFF232526),
            Color(0xFF414345),
            Color(0xFFDA22FF),
            Color(0xFF9733EE),
            Color(0xFF56CCF2)
        )
        "Solar Flare" -> listOf(
            Color(0xFFFF512F),
            Color(0xFFDD2476),
            Color(0xFFFFC837),
            Color(0xFFFF8008),
            Color(0xFFFF5F6D)
        )
        "Aurora Borealis" -> listOf(
            Color(0xFF232526),
            Color(0xFF1CD8D2),
            Color(0xFF93F9B9),
            Color(0xFF1FA2FF),
            Color(0xFF38F9D7)
        )
        else -> listOf(
            Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF9B42F2)
        )
    }

    MaterialTheme(colorScheme = if (theme.isDark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            floatingActionButton = {
                // Animated FAB with pulse
                val infiniteTransition = rememberInfiniteTransition(label = "fab")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fab_scale"
                )
                FloatingActionButton(
                    onClick = { nav.navigate(com.example.konvo.navigation.Dest.USER_SEARCH) },
                    containerColor = theme.groupAccent,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "New Chat", tint = Color.White)
                }
            },
            containerColor = theme.background
        ) { innerPadding ->
            // Parallax state
            var parallaxOffset by remember { mutableStateOf(Offset.Zero) }
            // Track drag for parallax
            val parallaxModifier = Modifier.pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    parallaxOffset += Offset(dragAmount.x, dragAmount.y)
                    change.consumeAllChanges()
                }
            }
            // Clamp parallax offset to a reasonable range
            val clampedParallax = Offset(
                x = parallaxOffset.x.coerceIn(-120f, 120f),
                y = parallaxOffset.y.coerceIn(-80f, 80f)
            )
            if (theme.animatedBackground && animatedThemeEnabled) {
                LoopingSlidingGradientBackground(colors = gradientColors) {
                    Column(Modifier.fillMaxSize()) {
                        // Top App Bar
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val anima = AnimatedGradient()
                            // LOGOUT BUTTON
                            IconButton(
                                onClick = {
                                    showLogoutDialog = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Logout",
                                    tint = Color.Red,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Image(
                                    painter = painterResource(R.drawable.logo_konvo_white),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContext.canvas.saveLayer(size.toRect(), Paint())
                                                drawContent()
                                                drawRect(brush = anima, blendMode = BlendMode.SrcIn)
                                                drawContext.canvas.restore()
                                            }
                                        }
                                )
                                Text(
                                    "Conversations, Reimagined",
                                    color = theme.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showThemeDialog = true }) {
                                Icon(
                                    Icons.Default.Brightness7,
                                    contentDescription = "Choose theme",
                                    tint = theme.groupAccent
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(contentAlignment = Alignment.BottomEnd) {
                                // Profile Avatar with image or fallback
                                if (userProfileImage != null) {
                                    println("[ChatListScreen] Loading profile image: $userProfileImage")
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(userProfileImage + "?t=${System.currentTimeMillis()}")
                                            .crossfade(true)
                                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                            .placeholder(android.R.drawable.ic_menu_gallery)
                                            .error(android.R.drawable.ic_menu_report_image)
                                            .build(),
                                        contentDescription = "Profile Image",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, userColor.copy(alpha = 0.3f), CircleShape)
                                            .clickable { 
                                                if (showProfileImagePreview == null) {
                                                    showProfileImagePreview = userProfileImage
                                                } else {
                                                    println("[ChatListScreen] Setting showProfileSettings to true")
                                                    showProfileSettings = true
                                                }
                                            },
                                        contentScale = ContentScale.Crop,
                                        onLoading = { println("[ChatListScreen] Loading profile image: $userProfileImage") },
                                        onSuccess = { println("[ChatListScreen] Successfully loaded profile image: $userProfileImage") },
                                        onError = { println("[ChatListScreen] Error loading profile image: $userProfileImage - ${it.result.throwable?.message}") }
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = userColor.copy(alpha = 0.1f),
                                        modifier = Modifier.size(36.dp)
                                            .clickable { 
                                                println("[ChatListScreen] Setting showProfileSettings to true (second instance)")
                                                showProfileSettings = true 
                                            }
                                    ) {
                                        Text(
                                            userInitials,
                                            color = userColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                                // Online status dot
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .background(
                                            if (userOnline) Color(0xFF4CAF50) else Color(
                                                0xFFB0B3C6
                                            ),
                                            CircleShape
                                        )
                                )
                            }
                        }
                        // Theme chooser dialog
                        if (showThemeDialog) {
                            AlertDialog(
                                onDismissRequest = { showThemeDialog = false },
                                title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        konvoThemes.forEachIndexed { idx, t ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        themeIndex = idx
                                                        showThemeDialog = false
                                                    }
                                                    .padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(28.dp)
                                                        .background(t.groupAccent, CircleShape)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    t.name,
                                                    fontWeight = if (themeIndex == idx) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (themeIndex == idx) {
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(
                                                        Icons.Default.Brightness7,
                                                        contentDescription = null,
                                                        tint = t.groupAccent
                                                    )
                                                }
                                            }
                                        }
                                        // Add the GALACTIC MODE toggle below the theme list if the selected theme supports animation
                                        if (konvoThemes[themeIndex].animatedBackground) {
                                            Spacer(Modifier.height(12.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.widthIn(max = 180.dp)
                                                    .align(Alignment.CenterHorizontally)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = konvoThemes[themeIndex].groupAccent,
                                                    modifier = Modifier.size(18.dp)
                                                        .shadow(2.dp, shape = CircleShape)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "GALACTIC MODE",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    style = LocalTextStyle.current.copy(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                konvoThemes[themeIndex].groupAccent,
                                                                konvoThemes[themeIndex].dmAccent,
                                                                konvoThemes[themeIndex].unreadBadge
                                                            )
                                                        )
                                                    ),
                                                    letterSpacing = 1.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.shadow(2.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Switch(
                                                    checked = animatedThemeEnabled,
                                                    onCheckedChange = { animatedThemeEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = konvoThemes[themeIndex].groupAccent,
                                                        uncheckedThumbColor = konvoThemes[themeIndex].textSecondary
                                                    ),
                                                    modifier = Modifier.scale(0.95f)
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showThemeDialog = false }) {
                                        Text("Close")
                                    }
                                },
                                containerColor = theme.dialogBackground
                            )
                        }
                        // Search Bar
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text("Search chats...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.groupAccent,
                                unfocusedBorderColor = theme.groupAccent.copy(alpha = 0.5f),
                                focusedTextColor = theme.textPrimary,
                                unfocusedTextColor = theme.textPrimary,
                                cursorColor = theme.groupAccent,
                                focusedLeadingIconColor = theme.groupAccent,
                                unfocusedLeadingIconColor = theme.groupAccent,
                                focusedPlaceholderColor = theme.textSecondary,
                                unfocusedPlaceholderColor = theme.textSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 0.dp)
                        )
                        // Chat List
                        if (filteredChats.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = theme.groupAccent,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No chats yet!",
                                        color = theme.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        "Start a new conversation.",
                                        color = theme.textSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredChats) { chat ->
                                    ChatListItem(
                                        chat = chat,
                                        groupAccent = theme.groupAccent,
                                        dmAccent = theme.dmAccent,
                                        unreadBadge = theme.unreadBadge,
                                        textPrimary = theme.textPrimary,
                                        textSecondary = theme.textSecondary,
                                        card = theme.card,
                                        onClick = { 
                                            val chatName = when (chat) {
                                                is GroupChat -> chat.groupName
                                                is DMChat -> chat.userUsername
                                            }
                                            val isGroupChat = chat is GroupChat
                                            val myUid = userId ?: ""
                                            val otherUid = when (chat) {
                                                is GroupChat -> "group"
                                                is DMChat -> chat.otherUserId
                                            }
                                            nav.navigate("chat/$myUid/$otherUid/$chatName/$isGroupChat")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().background(theme.background)) {
                    // Top App Bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val anima = AnimatedGradient()
                        // LOGOUT BUTTON
                        IconButton(
                            onClick = {
                                showLogoutDialog = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Logout",
                                tint = Color.Red,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Image(
                                painter = painterResource(R.drawable.logo_konvo_white),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(36.dp)
                                    .drawWithCache {
                                        onDrawWithContent {
                                            drawContext.canvas.saveLayer(size.toRect(), Paint())
                                            drawContent()
                                            drawRect(brush = anima, blendMode = BlendMode.SrcIn)
                                            drawContext.canvas.restore()
                                        }
                                    }
                            )
                            Text(
                                "Conversations, Reimagined",
                                color = theme.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showThemeDialog = true }) {
                            Icon(
                                Icons.Default.Brightness7,
                                contentDescription = "Choose theme",
                                tint = theme.groupAccent
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(contentAlignment = Alignment.BottomEnd) {
                            // Profile Avatar with image or fallback
                            if (userProfileImage != null) {
                                println("[ChatListScreen] Loading profile image: $userProfileImage")
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(userProfileImage + "?t=${System.currentTimeMillis()}")
                                        .crossfade(true)
                                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_menu_report_image)
                                        .build(),
                                    contentDescription = "Profile Image",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, userColor.copy(alpha = 0.3f), CircleShape)
                                        .clickable { 
                                            if (showProfileImagePreview == null) {
                                                showProfileImagePreview = userProfileImage
                                            } else {
                                                println("[ChatListScreen] Setting showProfileSettings to true (second instance)")
                                                showProfileSettings = true
                                            }
                                        },
                                    contentScale = ContentScale.Crop,
                                    onLoading = { println("[ChatListScreen] Loading profile image: $userProfileImage") },
                                    onSuccess = { println("[ChatListScreen] Successfully loaded profile image: $userProfileImage") },
                                    onError = { println("[ChatListScreen] Error loading profile image: $userProfileImage - ${it.result.throwable?.message}") }
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = userColor.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                        .clickable { 
                                            println("[ChatListScreen] Setting showProfileSettings to true (second instance)")
                                            showProfileSettings = true 
                                        }
                                ) {
                                    Text(
                                        userInitials,
                                        color = userColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                            // Online status dot
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(
                                        if (userOnline) Color(0xFF4CAF50) else Color(
                                            0xFFB0B3C6
                                        ), CircleShape
                                    )
                                    .border(2.dp, theme.background, CircleShape)
                            )
                        }
                    }
                    // Theme chooser dialog
                    if (showThemeDialog) {
                        AlertDialog(
                            onDismissRequest = { showThemeDialog = false },
                            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    konvoThemes.forEachIndexed { idx, t ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    themeIndex = idx
                                                    showThemeDialog = false
                                                }
                                                .padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(28.dp)
                                                    .background(t.groupAccent, CircleShape)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                t.name,
                                                fontWeight = if (themeIndex == idx) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (themeIndex == idx) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(
                                                    Icons.Default.Brightness7,
                                                    contentDescription = null,
                                                    tint = t.groupAccent
                                                )
                                            }
                                        }
                                    }
                                    // Add the GALACTIC MODE toggle below the theme list if the selected theme supports animation
                                    if (konvoThemes[themeIndex].animatedBackground) {
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                                .align(Alignment.CenterHorizontally)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = konvoThemes[themeIndex].groupAccent,
                                                modifier = Modifier.size(18.dp)
                                                    .shadow(2.dp, shape = CircleShape)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "GALACTIC MODE",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                style = LocalTextStyle.current.copy(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            konvoThemes[themeIndex].groupAccent,
                                                            konvoThemes[themeIndex].dmAccent,
                                                            konvoThemes[themeIndex].unreadBadge
                                                        )
                                                    )
                                                ),
                                                letterSpacing = 1.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.shadow(2.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Switch(
                                                checked = animatedThemeEnabled,
                                                onCheckedChange = { animatedThemeEnabled = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = konvoThemes[themeIndex].groupAccent,
                                                    uncheckedThumbColor = konvoThemes[themeIndex].textSecondary
                                                ),
                                                modifier = Modifier.scale(0.95f)
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showThemeDialog = false }) {
                                    Text("Close")
                                }
                            },
                            containerColor = theme.dialogBackground
                        )
                    }
                    // Search Bar
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search chats...") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.groupAccent,
                            unfocusedBorderColor = theme.groupAccent.copy(alpha = 0.5f),
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            cursorColor = theme.groupAccent,
                            focusedLeadingIconColor = theme.groupAccent,
                            unfocusedLeadingIconColor = theme.groupAccent,
                            focusedPlaceholderColor = theme.textSecondary,
                            unfocusedPlaceholderColor = theme.textSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 0.dp)
                    )
                    // Chat List
                    if (filteredChats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = theme.groupAccent,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No chats yet!",
                                    color = theme.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    "Start a new conversation.",
                                    color = theme.textSecondary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredChats) { chat ->
                                ChatListItem(
                                    chat = chat,
                                    groupAccent = theme.groupAccent,
                                    dmAccent = theme.dmAccent,
                                    unreadBadge = theme.unreadBadge,
                                    textPrimary = theme.textPrimary,
                                    textSecondary = theme.textSecondary,
                                    card = theme.card,
                                    onClick = { 
                                        val chatName = when (chat) {
                                            is GroupChat -> chat.groupName
                                            is DMChat -> chat.userUsername
                                        }
                                        val isGroupChat = chat is GroupChat
                                        val myUid = userId ?: ""
                                        val otherUid = when (chat) {
                                            is GroupChat -> "group"
                                            is DMChat -> chat.otherUserId
                                        }
                                        nav.navigate("chat/$myUid/$otherUid/$chatName/$isGroupChat")
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // Cool Logout Confirmation Dialog
            if (showLogoutDialog) {
                // Animated gradient for logout dialog
                val gradientTransition = rememberInfiniteTransition(label = "logout_gradient")
                val gradientProgress by gradientTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logout_gradient_progress"
                )
                // Interpolate between two color sets
                val colorTop = lerp(Color(0xFF23264A), Color(0xFFB71C1C), gradientProgress)
                val colorBottom = lerp(Color(0xFFB71C1C), Color(0xFF23264A), gradientProgress)
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sign Out?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Text(
                            "Are you sure you want to log out? You'll need to sign in again to access your chats.",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                signOutEverywhere(context) {
                                    scope.launch {
                                        nav.navigate(Dest.LOGIN) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .shadow(32.dp, RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(colorTop, colorBottom)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.Transparent
                )
            }
            // Profile dialog section removed - using ProfileSettingsScreen directly
        }
    }

    // Profile image preview dialog
    if (showProfileImagePreview != null) {
        Dialog(onDismissRequest = { showProfileImagePreview = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(showProfileImagePreview + "?t=${System.currentTimeMillis()}")
                        .crossfade(true)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .build(),
                    contentDescription = "Profile Image Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = { showProfileImagePreview = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GalacticParticles(
    modifier: Modifier = Modifier,
    starCount: Int = 48,
    shootingStarCount: Int = 3
) {
    val infinite = rememberInfiniteTransition(label = "particles")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )
    val density = LocalDensity.current
    val random = remember { java.util.Random(42) }
    // Shooting star state: random start time, speed, angle, and length
    data class ShootingStar(val start: Float, val duration: Float, val angle: Float, val length: Float, val y: Float)
    val shootingStars = remember {
        List(shootingStarCount) {
            ShootingStar(
                start = random.nextFloat(),
                duration = 0.18f + random.nextFloat() * 0.22f,
                angle = 60f + random.nextFloat() * 40f, // 60-100 degrees
                length = 180f + random.nextFloat() * 120f,
                y = 0.1f + random.nextFloat() * 0.8f
            )
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Draw twinkling stars
        repeat(starCount) { i ->
            val t = (time * 1.5f + i * 0.13f) % 1f
            val x = w * ((i * 73 % 100) / 100f + 0.15f * t) % w
            val y = h * ((i * 37 % 100) / 100f + 0.22f * (1f - t)) % h
            val baseAlpha = 0.18f + 0.5f * (0.5f + 0.5f * kotlin.math.sin((t + i) * 6.28f))
            val size = 1.5f + 2.5f * (0.5f + 0.5f * kotlin.math.sin((t + i * 0.3f) * 6.28f))
            drawCircle(
                color = Color.White.copy(alpha = baseAlpha),
                radius = size * density.density,
                center = Offset(x, y)
            )
        }
        // Draw improved shooting stars
        shootingStars.forEach { star ->
            val t = ((time - star.start + 1f) % 1f)
            if (t < star.duration) {
                val progress = t / star.duration
                val fade = if (progress < 0.2f) progress / 0.2f else if (progress > 0.8f) (1f - progress) / 0.2f else 1f
                val angleRad = Math.toRadians(star.angle.toDouble()).toFloat()
                val x0 = w * (0.1f + 0.8f * (star.start + progress) % 1f)
                val y0 = h * star.y
                val x1 = x0 + star.length * kotlin.math.cos(angleRad)
                val y1 = y0 + star.length * kotlin.math.sin(angleRad)
                drawLine(
                    color = Color.White.copy(alpha = 0.7f * fade),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = 2.5f * density.density,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatItem,
    groupAccent: Color,
    dmAccent: Color,
    unreadBadge: Color,
    textPrimary: Color,
    textSecondary: Color,
    card: Color,
    isGalacticMode: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var actionIconScale by remember { mutableStateOf(1f) }
    val animatedIconScale by animateFloatAsState(
        targetValue = actionIconScale,
        animationSpec = tween(200, easing = EaseOutBack),
        label = "icon_scale"
    )
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { newValue ->
            if (newValue == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
                false
            } else {
                true // allow all other transitions, including swipe-back
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(card)
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actionIconScale = 1.2f
                        },
                        modifier = Modifier.background(
                            Color.Red.copy(alpha = 0.15f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.scale(animatedIconScale)
                        )
                    }
                }
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    val scope = rememberCoroutineScope()
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable {
                                scope.launch { dismissState.reset() }
                            }
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actionIconScale = 1.2f
                                },
                                modifier = Modifier.background(
                                    groupAccent.copy(alpha = 0.15f),
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pin",
                                    tint = groupAccent,
                                    modifier = Modifier.scale(animatedIconScale)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actionIconScale = 1.2f
                                },
                                modifier = Modifier.background(
                                    dmAccent.copy(alpha = 0.15f),
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = "Mute",
                                    tint = dmAccent,
                                    modifier = Modifier.scale(animatedIconScale)
                                )
                            }
                        }
                    }
                }
            }
        },
        content = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = card,
                tonalElevation = if (isGalacticMode) 0.dp else 2.dp,
                shadowElevation = if (isGalacticMode) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Row(
                    Modifier
                        .then(if (isGalacticMode) Modifier.padding(horizontal = 4.dp, vertical = 4.dp) else Modifier.padding(horizontal = 12.dp, vertical = 10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    if (chat is GroupChat) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(groupAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = groupAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (chat is DMChat) {
                        if (chat.profileImage != null) {
                            // Display profile image if available
                            println("[ChatListScreen] Loading chat profile image: ${chat.profileImage}")
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(chat.profileImage + "?t=${System.currentTimeMillis()}")
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .build(),
                                contentDescription = "Profile image",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, dmAccent.copy(alpha = 0.3f), CircleShape),
                                contentScale = ContentScale.Crop,
                                onLoading = { println("[ChatListScreen] Loading chat profile image: ${chat.profileImage}") },
                                onSuccess = { println("[ChatListScreen] Successfully loaded chat profile image: ${chat.profileImage}") },
                                onError = { println("[ChatListScreen] Error loading chat profile image: ${chat.profileImage} - ${it.result.throwable?.message}") }
                            )
                        } else {
                            // Default avatar with first letter if no profile image
                            Box(
                                Modifier.size(48.dp).clip(CircleShape)
                                    .background(dmAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chat.userName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = dmAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Chat Info
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (chat) {
                                is GroupChat -> chat.groupName
                                is DMChat -> chat.userName
                            },
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when (chat) {
                                is GroupChat -> chat.lastMessage
                                is DMChat -> chat.lastMessage
                            },
                            color = textSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Time & Unread
                    Column(horizontalAlignment = Alignment.End) {
                        val formattedTime = try {
                            val rawTime = when (chat) {
                                is GroupChat -> chat.lastMessageTime
                                is DMChat -> chat.lastMessageTime
                                else -> ""
                            }
                            if (rawTime.isNotBlank() && rawTime.length > 8) {
                                val ts = rawTime.toLongOrNull() ?: 0L
                                if (ts > 1000000000000L) {
                                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata") // Set to Indian Standard Time
                                    sdf.format(Date(ts))
                                } else {
                                    rawTime
                                }
                            } else rawTime
                        } catch (e: Exception) { "" }
                        Text(
                            formattedTime,
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        val unreadCount = when (chat) {
                            is GroupChat -> chat.unread
                            is DMChat -> chat.unread
                        }
                        AnimatedVisibility(visible = unreadCount > 0) {
                            val scale by animateFloatAsState(
                                targetValue = if (unreadCount > 0) 1f else 0.7f,
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = EaseOutBack
                                ),
                                label = "badge_scale"
                            )
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .scale(scale)
                                    .background(unreadBadge, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${if (unreadCount > 99) "99+" else unreadCount}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    )
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = { Text("Delete Chat?") },
            text = { Text("Are you sure you want to delete this chat? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Delete chatlist entry for this user
                    val db = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        db.collection("users").document(userId).collection("chats").document(chat.id).delete()
                    }
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileScreen(onClose: () -> Unit) {
    val user = Firebase.auth.currentUser
    val uid = user?.uid
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Load profile on open
    LaunchedEffect(uid) {
        if (uid != null) {
            loading = true
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                username = doc.getString("username") ?: ""
                loaded = true
                loading = false
            }.addOnFailureListener {
                error = "Failed to load profile: ${it.message}"
                loading = false
            }
        }
    }

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
            usernameAvailable = query.isEmpty || (query.documents.firstOrNull()?.id == uid)
            loading = false
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
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
                )
            )
            if (loading && !loaded) {
                CircularProgressIndicator(Modifier.padding(top = 8.dp))
            } else if (username.isNotBlank() && username.length >= 3) {
                when (usernameAvailable) {
                    true -> Text("Username available", color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 8.dp))
                    false -> Text("Username taken", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    null -> {}
                }
            }
            if (error.isNotBlank()) {
                Text(error, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) { Text("Cancel", color = Color.Black) }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (uid == null) {
                            error = "User not signed in."
                            return@Button
                        }
                        if (name.isBlank() || username.isBlank() || usernameAvailable != true) {
                            error = "Please enter a valid name and available username."
                            return@Button
                        }
                        loading = true
                        error = ""
                        scope.launch {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(uid).update(
                                mapOf(
                                    "name" to name,
                                    "username" to username
                                )
                            ).addOnSuccessListener {
                                onClose()
                            }.addOnFailureListener {
                                error = "Failed to update profile: ${it.message}"
                                loading = false
                            }
                        }
                    },
                    enabled = name.isNotBlank() && username.isNotBlank() && usernameAvailable == true && !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FACFE))
                ) {
                    if (loading && loaded) CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp))
                    else Text("Save", color = Color.White)
                }
            }
        }
    }
}
