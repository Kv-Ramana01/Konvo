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
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.konvo.util.rememberSlidingBrush
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore setup
private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
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
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    val animatedBackground: Boolean = false // If true, use animated background brush
)

private val themes = listOf(
    KonvoTheme(
        name = "Classic Dark",
        background = Color(0xFF10142A),
        card = Color(0xFF23264A),
        groupAccent = KonvoBlue,
        dmAccent = KonvoOrangeDark,
        unreadBadge = Color(0xFF9B42F2),
        textPrimary = Color.White,
        textSecondary = Color(0xFFB0B3C6),
        isDark = true
    ),
    KonvoTheme(
        name = "Classic Light",
        background = Color(0xFFF7F8FA),
        card = Color.White,
        groupAccent = KonvoBlueDark,
        dmAccent = KonvoBlue,
        unreadBadge = KonvoBlueDark,
        textPrimary = Color(0xFF1A1A1A),
        textSecondary = Color(0xFF6B6B6B),
        isDark = false
    ),
    KonvoTheme(
        name = "Neon",
        background = Color(0xFF18122B),
        card = Color(0xFF393053),
        groupAccent = Color(0xFF00FFD0),
        dmAccent = Color(0xFF00B4FF),
        unreadBadge = Color(0xFFFF00E0),
        textPrimary = Color.White,
        textSecondary = Color(0xFFB0B3C6),
        isDark = true
    ),
    KonvoTheme(
        name = "Pastel",
        background = Color(0xFFF8E8EE),
        card = Color(0xFFFDEBED),
        groupAccent = Color(0xFFB1AFFF),
        dmAccent = Color(0xFFFFB3B3),
        unreadBadge = Color(0xFFB1AFFF),
        textPrimary = Color(0xFF2D3250),
        textSecondary = Color(0xFF6B6B6B),
        isDark = false
    ),
    KonvoTheme(
        name = "Sunset",
        background = Color(0xFFFFE5D9),
        card = Color(0xFFFFB4A2),
        groupAccent = Color(0xFFFF6F61),
        dmAccent = Color(0xFF6B705C),
        unreadBadge = Color(0xFFFF6F61),
        textPrimary = Color(0xFF2D1E2F),
        textSecondary = Color(0xFF6B6B6B),
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
        textPrimary = Color(0xFFE0E7FF),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFFFF6F6),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFE0F7FA),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFF5F6FA),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFF8FFAE),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFFFF6F6),
        textSecondary = Color(0xFFB0B3C6),
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
        textPrimary = Color(0xFFF8FFAE),
        textSecondary = Color(0xFFB0B3C6),
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

    // Mock user profile
    val userName = "John Doe"
    val userOnline = true
    val userInitials = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
    val userColor = Color(0xFF4FACFE)

    // Mock chat data
    val chats = listOf(
        GroupChat("Study Group", "See you at 7pm!", "09:12", 2),
        DMChat("Alice", "Got it, thanks!", "08:45", 0),
        GroupChat("Family", "Dinner's ready!", "08:30", 1),
        DMChat("Bob", "Let's catch up soon.", "Yesterday", 3),
        DMChat("Charlie", "👍", "Yesterday", 0),
        GroupChat("Project X", "Final version sent.", "Mon", 0),
        // Add more mock chats for scroll test
        DMChat("David", "See you tomorrow!", "Sun", 1),
        GroupChat("Work", "Meeting at 10am", "Sun", 0),
        DMChat("Eve", "Check this out!", "Sat", 2),
        GroupChat("Friends", "Movie night?", "Fri", 0),
        DMChat("Frank", "Thanks!", "Thu", 0),
        GroupChat("Gaming", "GG!", "Wed", 4),
        DMChat("Grace", "On my way.", "Tue", 0),
        GroupChat("Book Club", "Next book?", "Mon", 0),
        DMChat("Hannah", "Congrats!", "Sun", 1),
        GroupChat("Travel", "Tickets booked.", "Sat", 0),
        DMChat("Ivan", "Happy Birthday!", "Fri", 0),
        GroupChat("Music", "New album out!", "Thu", 2),
        DMChat("Judy", "See you soon.", "Wed", 0),
        GroupChat("Sports", "Game tonight!", "Tue", 3),
        DMChat("Kevin", "Let's meet.", "Mon", 0),
        GroupChat("Family 2", "Picnic this weekend?", "Sun", 0),
        DMChat("Laura", "Call me.", "Sat", 0),
        GroupChat("Dev Team", "Code review done.", "Fri", 1),
        DMChat("Mallory", "Lunch?", "Thu", 0),
        GroupChat("Startup", "Pitch deck ready.", "Wed", 0),
        DMChat("Nina", "Awesome!", "Tue", 0),
        GroupChat("Volunteers", "Event tomorrow.", "Mon", 2),
        DMChat("Oscar", "Good luck!", "Sun", 0),
        GroupChat("Neighbors", "Party at 8!", "Sat", 0)
    ).filter {
        search.isBlank() ||
        (it is GroupChat && it.groupName.contains(search, true)) ||
        (it is DMChat && it.userName.contains(search, true))
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
            val animatedBrush = if (theme.animatedBackground && animatedThemeEnabled) {
                val gradientColors = when (theme.name) {
                    "Galactic Aurora" -> listOf(
                        Color(0xFF0B0033), Color(0xFF3700B3), Color(0xFF00FFB3), Color(0xFF00CFFF), Color(0xFFB388FF)
                    )
                    "Cosmic Sunset" -> listOf(
                        Color(0xFF1A0A2D), Color(0xFF3D155F), Color(0xFFFF6F91), Color(0xFFFF9671), Color(0xFFFFC75F)
                    )
                    "Stellar Ice" -> listOf(
                        Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF36D1C4), Color(0xFF5B86E5), Color(0xFFB2FEFA)
                    )
                    "Cyberpunk" -> listOf(
                        Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFFFF2E63), Color(0xFF08D9D6), Color(0xFFFFC300)
                    )
                    "Nebula Dream" -> listOf(
                        Color(0xFF232526), Color(0xFF414345), Color(0xFFDA22FF), Color(0xFF9733EE), Color(0xFF56CCF2)
                    )
                    "Solar Flare" -> listOf(
                        Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFFFFC837), Color(0xFFFF8008), Color(0xFFFF5F6D)
                    )
                    "Aurora Borealis" -> listOf(
                        Color(0xFF232526), Color(0xFF1CD8D2), Color(0xFF93F9B9), Color(0xFF1FA2FF), Color(0xFF38F9D7)
                    )
                    else -> listOf(
                        Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF9B42F2)
                    )
                }
                rememberSlidingBrush(colors = gradientColors)
            } else null
            Column(
                Modifier.fillMaxSize().then(
                    if (animatedBrush != null) Modifier.background(animatedBrush) else Modifier.background(theme.background)
                )
            ) {
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
                    // Animated theme toggle (only if theme supports it)
                    if (theme.animatedBackground) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Animated", color = theme.textSecondary, fontSize = 13.sp)
                            Switch(
                                checked = animatedThemeEnabled,
                                onCheckedChange = { animatedThemeEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.groupAccent,
                                    uncheckedThumbColor = theme.textSecondary
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Brightness7, contentDescription = "Choose theme", tint = theme.groupAccent)
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
                                .background(if (userOnline) Color(0xFF4CAF50) else Color(0xFFB0B3C6), CircleShape)
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
                                        Text(t.name, fontWeight = if (themeIndex == idx) FontWeight.Bold else FontWeight.Normal)
                                        if (themeIndex == idx) {
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.Brightness7, contentDescription = null, tint = t.groupAccent)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Close")
                            }
                        }
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
                if (chats.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = theme.groupAccent, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No chats yet!", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Start a new conversation.", color = theme.textSecondary, fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chats) { chat ->
                            ChatListItem(
                                chat = chat,
                                groupAccent = theme.groupAccent,
                                dmAccent = theme.dmAccent,
                                unreadBadge = theme.unreadBadge,
                                textPrimary = theme.textPrimary,
                                textSecondary = theme.textSecondary,
                                card = theme.card,
                                onClick = { /* TODO: Open chat */ }
                            )
                        }
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
                    Text("Sign Out?", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
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

@Composable
fun ChatListItem(
    chat: ChatItem,
    groupAccent: Color,
    dmAccent: Color,
    unreadBadge: Color,
    textPrimary: Color,
    textSecondary: Color,
    card: Color,
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
            // Show action backgrounds for swipe directions
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
                        modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.scale(animatedIconScale))
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
                                .background(card)
                                .padding(end = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    actionIconScale = 1.2f
                                },
                                modifier = Modifier.background(groupAccent.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = groupAccent, modifier = Modifier.scale(animatedIconScale))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    actionIconScale = 1.2f
                                },
                                modifier = Modifier.background(dmAccent.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = "Mute", tint = dmAccent, modifier = Modifier.scale(animatedIconScale))
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
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    if (chat is GroupChat) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(groupAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = groupAccent, modifier = Modifier.size(28.dp))
                        }
                    } else if (chat is DMChat) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(dmAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = dmAccent, modifier = Modifier.size(28.dp))
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
                                animationSpec = tween(durationMillis = 400, easing = EaseOutBack),
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
