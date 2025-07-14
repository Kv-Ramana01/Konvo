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
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.ui.theme.KonvoNavyLight
import com.example.konvo.ui.theme.KonvoOrangeDark
import androidx.navigation.NavController
import com.example.konvo.feature.auth.vm.signOutEverywhere
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
sealed class ChatItem(val id: String)
data class GroupChat(
    val groupName: String,
    val lastMessage: String,
    val time: String,
    val unread: Int
) : ChatItem(groupName)
data class DMChat(
    val userName: String,
    val lastMessage: String,
    val time: String,
    val unread: Int
) : ChatItem(userName)

// Theme data class
private data class KonvoTheme(
    val name: String,
    val background: Color,
    val card: Color,
    val groupAccent: Color,
    val dmAccent: Color,
    val unreadBadge: Color,
    val isDark: Boolean,
    val animatedBackground: Boolean = false
) {
    // Automatic contrast text colors
    val textPrimary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    val textSecondary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF6B6B6B) else Color(0xFFB0B3C6)
    // Dialog background: neutral and readable
    val dialogBackground: Color
        get() = if (background.luminance() > 0.5f) Color.White.copy(alpha = 0.97f) else Color(0xFF18122B)
}

private val themes = listOf(
    KonvoTheme(
        name = "Classic Dark",
        background = Color(0xFF10142A),
        card = Color(0xFF23264A),
        groupAccent = KonvoBlue,
        dmAccent = KonvoOrangeDark,
        unreadBadge = Color(0xFF9B42F2),
        isDark = true
    ),
    KonvoTheme(
        name = "Classic Light",
        background = Color(0xFFF7F8FA),
        card = Color.White,
        groupAccent = KonvoBlueDark,
        dmAccent = KonvoBlue,
        unreadBadge = KonvoBlueDark,
        isDark = false
    ),
    KonvoTheme(
        name = "Neon",
        background = Color(0xFF18122B),
        card = Color(0xFF393053),
        groupAccent = Color(0xFF00FFD0),
        dmAccent = Color(0xFF00B4FF),
        unreadBadge = Color(0xFFFF00E0),
        isDark = true
    ),
    KonvoTheme(
        name = "Pastel",
        background = Color(0xFFF8E8EE),
        card = Color(0xFFFDEBED),
        groupAccent = Color(0xFFB1AFFF),
        dmAccent = Color(0xFFFFB3B3),
        unreadBadge = Color(0xFFB1AFFF),
        isDark = false
    ),
    KonvoTheme(
        name = "Sunset",
        background = Color(0xFFFFE5D9),
        card = Color(0xFFFFB4A2),
        groupAccent = Color(0xFFFF6F61),
        dmAccent = Color(0xFF6B705C),
        unreadBadge = Color(0xFFFF6F61),
        isDark = false
    ),
    // --- New Ultra Aesthetic Galactic Themes ---
    KonvoTheme(
        name = "Galactic Aurora",
        background = Color(0xFF0B0033),
        card = Color(0xFF3700B3),
        groupAccent = Color(0xFF00FFB3),
        dmAccent = Color(0xFF00CFFF),
        unreadBadge = Color(0xFFB388FF),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Cosmic Sunset",
        background = Color(0xFF1A0A2D),
        card = Color(0xFF3D155F),
        groupAccent = Color(0xFFFF6F91),
        dmAccent = Color(0xFFFF9671),
        unreadBadge = Color(0xFFFFC75F),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Stellar Ice",
        background = Color(0xFF0F2027),
        card = Color(0xFF2C5364),
        groupAccent = Color(0xFF36D1C4),
        dmAccent = Color(0xFF5B86E5),
        unreadBadge = Color(0xFFB2FEFA),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Cyberpunk",
        background = Color(0xFF1A1A2E),
        card = Color(0xFF16213E),
        groupAccent = Color(0xFFFF2E63),
        dmAccent = Color(0xFF08D9D6),
        unreadBadge = Color(0xFFFFC300),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Nebula Dream",
        background = Color(0xFF232526),
        card = Color(0xFF414345),
        groupAccent = Color(0xFFDA22FF),
        dmAccent = Color(0xFF9733EE),
        unreadBadge = Color(0xFF56CCF2),
        isDark = true,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Solar Flare",
        background = Color(0xFFFF512F),
        card = Color(0xFFDD2476),
        groupAccent = Color(0xFFFFC837),
        dmAccent = Color(0xFFFF8008),
        unreadBadge = Color(0xFFFF5F6D),
        isDark = false,
        animatedBackground = true
    ),
    KonvoTheme(
        name = "Aurora Borealis",
        background = Color(0xFF232526),
        card = Color(0xFF1CD8D2),
        groupAccent = Color(0xFF93F9B9),
        dmAccent = Color(0xFF1FA2FF),
        unreadBadge = Color(0xFF38F9D7),
        isDark = false,
        animatedBackground = true
    )
)

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
    val theme = themes[themeIndex]
    var search by rememberSaveable { mutableStateOf("") }
    // Animated theme toggle state
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Real-time Firestore chat list
    val userId = Firebase.auth.currentUser?.uid
    var chats by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var listenerRegistration by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }

    DisposableEffect(userId) {
        listenerRegistration?.remove()
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("chats").document(userId).collection("chats")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        chats = snapshot.documents.mapNotNull { doc ->
                            val chatName = doc.id
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val time = doc.getString("lastMessageTime") ?: ""
                            val unread = (doc.getLong("unreadCount") ?: 0L).toInt()
                            DMChat(chatName, lastMessage, time, unread)
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Filter chats by search
    val filteredChats = chats.filter {
        search.isBlank() ||
                (it is GroupChat && it.groupName.contains(search, true)) ||
                (it is DMChat && it.userName.contains(search, true))
    }

    // Mock user profile
    val userName = "John Doe"
    val userOnline = true
    val userInitials =
        userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
            .uppercase()
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
                    onClick = { /* TODO: New chat */ },
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
                                Surface(
                                    shape = CircleShape,
                                    color = userColor.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(
                                        userInitials,
                                        color = userColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(6.dp)
                                    )
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
                                        themes.forEachIndexed { idx, t ->
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
                                        if (themes[themeIndex].animatedBackground) {
                                            Spacer(Modifier.height(12.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.widthIn(max = 180.dp)
                                                    .align(Alignment.CenterHorizontally)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = themes[themeIndex].groupAccent,
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
                                                                themes[themeIndex].groupAccent,
                                                                themes[themeIndex].dmAccent,
                                                                themes[themeIndex].unreadBadge
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
                                                        checkedThumbColor = themes[themeIndex].groupAccent,
                                                        uncheckedThumbColor = themes[themeIndex].textSecondary
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
                                                is DMChat -> chat.userName
                                            }
                                            val isGroupChat = chat is GroupChat
                                            nav.navigate("chat/${chat.id}/${chatName}/${isGroupChat}")
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
                            Surface(
                                shape = CircleShape,
                                color = userColor.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    userInitials,
                                    color = userColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
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
                                    themes.forEachIndexed { idx, t ->
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
                                    if (themes[themeIndex].animatedBackground) {
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                                .align(Alignment.CenterHorizontally)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = themes[themeIndex].groupAccent,
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
                                                            themes[themeIndex].groupAccent,
                                                            themes[themeIndex].dmAccent,
                                                            themes[themeIndex].unreadBadge
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
                                                    checkedThumbColor = themes[themeIndex].groupAccent,
                                                    uncheckedThumbColor = themes[themeIndex].textSecondary
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
                                            is DMChat -> chat.userName
                                        }
                                        val isGroupChat = chat is GroupChat
                                        nav.navigate("chat/${chat.id}/${chatName}/${isGroupChat}")
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
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(dmAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = dmAccent,
                                modifier = Modifier.size(28.dp)
                            )
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
                        Text(
                            when (chat) {
                                is GroupChat -> chat.time
                                is DMChat -> chat.time
                            },
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
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    // TODO: Delete logic
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
