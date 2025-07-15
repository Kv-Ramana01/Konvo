package com.example.konvo.feature.chat.ui

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.ui.theme.KonvoOrangeDark
import com.example.konvo.util.findActivity
import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.edit
import com.example.konvo.util.themeDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog

import android.content.Intent
import android.provider.Browser
import androidx.core.net.toUri
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.tasks.await

import com.example.konvo.ui.theme.KonvoTheme
import com.example.konvo.ui.theme.konvoThemes
import com.example.konvo.feature.user.get1to1ChatId
import com.example.konvo.data.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// Helper lerp for Float (since Compose's is internal)
private fun lerp(start: Float, stop: Float, fraction: Float): Float = (start * (1 - fraction) + stop * fraction)

// Data classes for chat messages
enum class MessageType(val value: String) {
    TEXT("text"), 
    IMAGE("image"), 
    DOCUMENT("document"),
    AUDIO("audio"),
    VIDEO("video"),
    LOCATION("location"),
    CONTACT("contact"),
    STICKER("sticker"),
    GIF("gif");
    
    companion object {
        fun fromString(value: String?): MessageType = when (value) {
            "image" -> IMAGE
            "document" -> DOCUMENT
            "audio" -> AUDIO
            "video" -> VIDEO
            "location" -> LOCATION
            "contact" -> CONTACT
            "sticker" -> STICKER
            "gif" -> GIF
            else -> TEXT
        }
    }
}

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val senderName: String = "",
    val isGroupChat: Boolean = false,
    val messageStatus: MessageStatus = MessageStatus.SENT,
    val messageType: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val replyTo: ReplyMessage? = null,
    val reactions: Map<String, String> = emptyMap(), // userId to emoji
    val audioDuration: Int? = null, // in seconds
    val videoDuration: Int? = null, // in seconds
    val location: LocationData? = null,
    val contact: ContactData? = null,
    val isForwarded: Boolean = false,
    val forwardedFrom: String? = null,
    val isEdited: Boolean = false,
    val editTimestamp: Long? = null,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deliveredAt: Long? = null, // NEW
    val readAt: Long? = null, // NEW
    val deletedFor: List<String> = emptyList()
)

data class ReplyMessage(
    val id: String,
    val text: String,
    val senderName: String,
    val messageType: MessageType
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class ContactData(
    val name: String,
    val phoneNumber: String,
    val email: String? = null
)

// Update MessageStatus enum for Firestore string mapping
enum class MessageStatus(val value: String) {
    SENDING("sending"), SENT("sent"), DELIVERED("delivered"), READ("read");
    companion object {
        fun fromString(value: String?): MessageStatus = when (value) {
            "sent" -> SENT
            "delivered" -> DELIVERED
            "read" -> READ
            else -> SENDING
        }
    }
}

data class ChatInfo(
    val chatId: String,
    val name: String,
    val isGroup: Boolean,
    val memberCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val profileImage: String? = null
)

// Theme data class (reusing from ChatListScreen)
data class KonvoTheme(
    val name: String,
    val background: Color,
    val card: Color,
    val groupAccent: Color,
    val dmAccent: Color,
    val unreadBadge: Color,
    val isDark: Boolean,
    val animatedBackground: Boolean = false
) {
    val textPrimary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    val textSecondary: Color
        get() = if (background.luminance() > 0.5f) Color(0xFF6B6B6B) else Color(0xFFB0B3C6)
    val messageBubbleMe: Color
        get() = if (isDark) groupAccent else groupAccent
    val messageBubbleOther: Color
        get() = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val inputBackground: Color
        get() = if (isDark) Color(0xFF2A2A2A) else Color.White
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

// DataStore setup (copied from ChatListScreen for now)
private val THEME_INDEX_KEY = androidx.datastore.preferences.core.intPreferencesKey("theme_index")
private val ANIMATED_THEME_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("animated_theme_enabled")

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

    // --- WhatsApp-Style Attachment Menu ---
@Composable
fun WhatsAppAttachmentMenu(
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onCameraClick: () -> Unit,
    onVideoCameraClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLocationClick: () -> Unit,
    onContactClick: () -> Unit,
    onStickerClick: () -> Unit,
    onGifClick: () -> Unit,
    theme: KonvoTheme
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = theme.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Share",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.textSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Attachment options grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Photo,
                            label = "Gallery",
                            onClick = onImageClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = onVideoClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Description,
                            label = "Document",
                            onClick = onDocumentClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.CameraAlt,
                            label = "Camera",
                            onClick = onCameraClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = onVideoCameraClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Mic,
                            label = "Audio",
                            onClick = onAudioClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.LocationOn,
                            label = "Location",
                            onClick = onLocationClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Person,
                            label = "Contact",
                            onClick = onContactClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.EmojiEmotions,
                            label = "Sticker",
                            onClick = onStickerClick,
                            theme = theme
                        )
                    }
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Gif,
                            label = "GIF",
                            onClick = onGifClick,
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    theme: KonvoTheme
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = theme.groupAccent.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = theme.groupAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = theme.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// --- WhatsApp-Style Reaction Picker ---
@Composable
fun ReactionPicker(
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    theme: KonvoTheme
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.card,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("❤️", "👍", "👎", "😂", "😮", "😢", "😡").forEach { emoji ->
                    Text(
                        emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { 
                                onReactionSelected(emoji)
                                onDismiss()
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// --- WhatsApp-Style Message Options Menu ---
@Composable
fun MessageOptionsMenu(
    message: ChatMessage,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    theme: KonvoTheme
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.card,
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                MessageOptionItem(
                    icon = Icons.Default.Reply,
                    label = "Reply",
                    onClick = {
                        onReply()
                        onDismiss()
                    },
                    theme = theme
                )
                MessageOptionItem(
                    icon = Icons.Default.Share,
                    label = "Forward",
                    onClick = {
                        onForward()
                        onDismiss()
                    },
                    theme = theme
                )
                MessageOptionItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = {
                        onCopy()
                        onDismiss()
                    },
                    theme = theme
                )
                MessageOptionItem(
                    icon = if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (message.isStarred) "Unstar" else "Star",
                    onClick = {
                        onStar()
                        onDismiss()
                    },
                    theme = theme
                )
                if (message.isFromMe) {
                    MessageOptionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        onClick = {
                            onEdit()
                            onDismiss()
                        },
                        theme = theme
                    )
                }
                MessageOptionItem(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    theme = theme,
                    textColor = Color.Red
                )
            }
        }
    }
}

@Composable
fun MessageOptionItem(
    icon: ImageVector,
    label: String,
    onClick:() -> Unit,
    theme: KonvoTheme,
    textColor: Color = theme.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            color = textColor,
            fontSize = 16.sp
        )
    }
}

// --- Single Message Options Menu ---
@Composable
fun SingleMessageOptionsMenu(
    message: ChatMessage,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onInfo: () -> Unit,
    onDismiss: () -> Unit,
    theme: KonvoTheme
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.card,
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                MessageOptionItem(
                    icon = Icons.Default.Edit,
                    label = "Edit",
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    theme = theme
                )
                MessageOptionItem(
                    icon = Icons.Default.PushPin,
                    label = "Pin",
                    onClick = {
                        onPin()
                        onDismiss()
                    },
                    theme = theme
                )
                MessageOptionItem(
                    icon = Icons.Default.Info,
                    label = "Info",
                    onClick = {
                        onInfo()
                        onDismiss()
                    },
                    theme = theme
                )
            }
        }
    }
}

// --- Message Info Dialog ---
@Composable
fun MessageInfoDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    theme: KonvoTheme
) {
    val timeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata") // Set to Indian Standard Time
    }
    val sentTime = timeFormat.format(Date(message.timestamp))
    val deliveredTime = message.deliveredAt?.let { timeFormat.format(Date(it)) } ?: "-"
    val readTime = message.readAt?.let { timeFormat.format(Date(it)) } ?: "-"
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.card,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Message Info",
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                InfoRow("Sent", sentTime, theme)
                InfoRow("Delivered", deliveredTime, theme)
                InfoRow("Read", readTime, theme)
                if (message.isEdited) {
                    InfoRow("Edited", "Yes", theme)
                }
                if (message.isForwarded) {
                    InfoRow("Forwarded from", message.forwardedFrom ?: "Unknown", theme)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.groupAccent)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, theme: KonvoTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = theme.textSecondary,
            fontSize = 14.sp
        )
        Text(
            value,
            color = theme.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- GLASSY/GRADIENT BACKGROUND ---
@Composable
fun ChatBackground(theme: KonvoTheme, content: @Composable BoxScope.() -> Unit) {
    if (theme.animatedBackground) {
        // Use the same gradient as ChatListScreen
        val gradientColors = when (theme.name) {
            "Galactic Aurora" -> listOf(Color(0xFF0B0033), Color(0xFF3700B3), Color(0xFF00FFB3), Color(0xFF00CFFF), Color(0xFFB388FF))
            "Cosmic Sunset" -> listOf(Color(0xFF1A0A2D), Color(0xFF3D155F), Color(0xFFFF6F91), Color(0xFFFF9671), Color(0xFFFFC75F))
            "Stellar Ice" -> listOf(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF36D1C4), Color(0xFF5B86E5), Color(0xFFB2FEFA))
            "Cyberpunk" -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFFFF2E63), Color(0xFF08D9D6), Color(0xFFFFC300))
            "Nebula Dream" -> listOf(Color(0xFF232526), Color(0xFF414345), Color(0xFFDA22FF), Color(0xFF9733EE), Color(0xFF56CCF2))
            "Solar Flare" -> listOf(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFFFFC837), Color(0xFFFF8008), Color(0xFFFF5F6D))
            "Aurora Borealis" -> listOf(Color(0xFF232526), Color(0xFF1CD8D2), Color(0xFF93F9B9), Color(0xFF1FA2FF), Color(0xFF38F9D7))
            else -> listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF9B42F2))
        }
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(gradientColors)),
            content = content
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(theme.background),
            content = content
        )
    }
}

// --- Enhanced WhatsApp-Style Top Bar ---
@Composable
fun ModernChatTopBar(
    chatInfo: ChatInfo, 
    theme: KonvoTheme, 
    onBackClick: () -> Unit, 
    onMoreClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onInfoClick: () -> Unit,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onSelectionCancel: () -> Unit = {},
    onStarSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onForwardSelected: () -> Unit = {},
    onSingleMessageMore: () -> Unit = {}
) {
    val context = LocalContext.current
    var showProfilePreview by remember { mutableStateOf<String?>(null) }
    
    Surface(
        color = theme.card.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                // Selection mode ONLY
                IconButton(onClick = onSelectionCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = theme.textPrimary)
                }
                Text(
                    "$selectedCount selected",
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onStarSelected) {
                    Icon(Icons.Default.Star, contentDescription = "Star", tint = theme.groupAccent)
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
                IconButton(onClick = onForwardSelected) {
                    Icon(Icons.Default.Share, contentDescription = "Forward", tint = theme.groupAccent)
                }
                if (selectedCount == 1) {
                    IconButton(onClick = onSingleMessageMore) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = theme.textPrimary)
                    }
                }
            } else {
                // Normal mode ONLY
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.textPrimary)
                }
                
                if (chatInfo.profileImage != null) {
                    // Display profile image
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(chatInfo.profileImage + "?t=${System.currentTimeMillis()}")
                            .crossfade(true)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .build(),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, if (chatInfo.isGroup) theme.groupAccent else theme.dmAccent, CircleShape)
                            .clickable { showProfilePreview = chatInfo.profileImage },
                        contentScale = ContentScale.Crop,
                        onLoading = { println("[ChatScreen] Loading profile image in top bar: ${chatInfo.profileImage}") },
                        onSuccess = { println("[ChatScreen] Successfully loaded profile image in top bar: ${chatInfo.profileImage}") },
                        onError = { println("[ChatScreen] Error loading profile image in top bar: ${chatInfo.profileImage} - ${it.result.throwable?.message}") }
                    )
                } else {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(
                            if (chatInfo.isGroup) theme.groupAccent.copy(alpha = 0.2f) else theme.dmAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (chatInfo.isGroup) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (chatInfo.isGroup) theme.groupAccent else theme.dmAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        chatInfo.name, 
                        color = theme.textPrimary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chatInfo.isGroup) {
                        Text("${chatInfo.memberCount} members", color = theme.textSecondary, fontSize = 12.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (chatInfo.isOnline) Color(0xFF4CAF50) else Color(0xFFB0B3C6)))
                            Spacer(Modifier.width(4.dp))
                            Text(if (chatInfo.isOnline) "Online" else "Offline", color = theme.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.textPrimary)
                }
                IconButton(onClick = onCallClick) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = theme.groupAccent)
                }
                IconButton(onClick = onVideoClick) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = theme.groupAccent)
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = theme.textPrimary)
                }
            }
        }
    }
    
    // Profile image preview dialog
    if (showProfilePreview != null) {
        Dialog(onDismissRequest = { showProfilePreview = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(showProfilePreview + "?t=${System.currentTimeMillis()}")
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
                    onClick = { showProfilePreview = null },
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

// --- Enhanced WhatsApp-Style Message Bubbles ---
@Composable
fun PrettyMessageBubble(
    message: ChatMessage,
    theme: KonvoTheme,
    isGroupChat: Boolean,
    showAvatar: Boolean = false,
    onLongPress: () -> Unit,
    onImageClick: (String?) -> Unit,
    onReactionClick: (String) -> Unit = {},
    onMessageOptions: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {}
) {
    val isFromMe = message.isFromMe
    val textColor = if (isFromMe) Color.White else Color(0xFF222222)
    val timeColor = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color(0xFF6B6B6B)
    val shadow = if (isFromMe) 4.dp else 1.dp
    val tailAlignment = if (isFromMe) Alignment.BottomEnd else Alignment.BottomStart
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata") // Set to Indian Standard Time
    }
    val messageTime = timeFormat.format(Date(message.timestamp))
    val selectionColor = if (isSelected) theme.groupAccent.copy(alpha = 0.18f) else Color.Transparent
    val starVisible = message.isStarred
    val starAlpha by animateFloatAsState(targetValue = if (starVisible) 1f else 0f, label = "star_alpha")
    val starScale by animateFloatAsState(targetValue = if (starVisible) 1f else 0.7f, label = "star_scale")
    Row(
        Modifier
            .fillMaxWidth()
            .background(selectionColor)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectionToggle() else onLongPress()
                },
                onLongClick = {
                    onLongPress()
                }
            )
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromMe && showAvatar) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(theme.dmAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(message.senderName.take(2).uppercase(), color = theme.dmAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(4.dp))
        } else if (!isFromMe) {
            Spacer(Modifier.width(4.dp))
        }
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val maxBubbleWidth = screenWidth * 0.8f
        BoxWithConstraints {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = tailAlignment
            ) {
                Box {
                    if (message.messageType == MessageType.TEXT) {
                        ModernTextBubble(
                            message = message.text,
                            time = messageTime,
                            isFromMe = isFromMe,
                            status = message.messageStatus,
                            isEdited = message.isEdited
                        )
                    }
                    else if (message.messageType == MessageType.IMAGE) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp,
                            color = Color.Transparent,
                            modifier = Modifier
                                .widthIn(max = screenWidth * 0.6f)
                                .wrapContentWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { onLongPress() },
                                        onTap = { if (isSelectionMode) onSelectionToggle() else onImageClick(message.mediaUrl) }
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(message.mediaUrl + "?t=${System.currentTimeMillis()}")
                                            .crossfade(true)
                                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                            .placeholder(android.R.drawable.ic_menu_gallery)
                                            .error(android.R.drawable.ic_menu_report_image)
                                            .build(),
                                        contentDescription = "Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = messageTime,
                                            color = Color.White,
                                            fontSize = 10.sp
                                        )
                                        if (isFromMe) {
                                            Spacer(Modifier.width(3.dp))
                                            MessageStatusIcon(message.messageStatus, Color.White)
                                        }
                                    }
                                }
                                if (message.text.isNotEmpty()) {
                                    Text(
                                        message.text,
                                        color = if (isFromMe) Color.White else Color(0xFF222222),
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isFromMe) theme.groupAccent else theme.card,
                                                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    else if (message.messageType == MessageType.DOCUMENT) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp,
                            color = if (isFromMe) theme.groupAccent else theme.card,
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .wrapContentWidth()
                                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
                        ) {
                            DocumentPreview(
                                fileName = message.fileName ?: "Document",
                                fileSize = message.fileSize,
                                theme = theme,
                                textColor = if (isFromMe) Color.White else theme.textPrimary,
                                mediaUrl = message.mediaUrl
                            )
                        }
                    }
                    // --- Starred Icon Overlay ---
                    if (starVisible) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .size(24.dp)
                                .graphicsLayer {
                                    alpha = starAlpha
                                    scaleX = starScale
                                    scaleY = starScale
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = Color(0xFFFFD600), // Gold
                                modifier = Modifier.size(22.dp).shadow(2.dp, CircleShape)
                            )
                        }
                    }
                }
            }
        }
        if (isFromMe) Spacer(Modifier.width(2.dp))
    }
}

// --- Enhanced WhatsApp-Style Input Bar ---
@Composable
fun FloatingInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordStop: () -> Unit,
    isRecording: Boolean = false,
    recordingDuration: Int = 0,
    replyTo: ChatMessage? = null,
    onReplyCancel: () -> Unit = {},
    theme: KonvoTheme,
    focusRequester: FocusRequester
) {
    Surface(
        color = theme.card.copy(alpha = 0.92f),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachmentClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = theme.textSecondary)
            }
            IconButton(onClick = onEmojiClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = theme.textSecondary)
            }
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .heightIn(min = 48.dp, max = 160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    cursorColor = theme.groupAccent,
                    focusedPlaceholderColor = theme.textSecondary,
                    unfocusedPlaceholderColor = theme.textSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                minLines = 1,
                maxLines = 6
            )
            // Animated send button
            val enabled = messageText.trim().isNotEmpty()
            val scale by animateFloatAsState(targetValue = if (enabled) 1.15f else 1f, animationSpec = tween(300), label = "send_scale")
            IconButton(
                onClick = onSendClick,
                modifier = Modifier.size(40.dp).scale(scale),
                enabled = enabled
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = if (enabled) theme.groupAccent else theme.textSecondary)
            }
        }
    }
}

// --- GLASSY/BLURRED HEADER WITH ACTION ICONS ---
@Composable
fun KonvoChatHeader(
    chatInfo: ChatInfo,
    theme: KonvoTheme,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current
    var showProfilePreview by remember { mutableStateOf<String?>(null) }
    
    Surface(
        color = theme.card.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.textPrimary)
            }
            // Animated green gradient ring for online status
            if (!chatInfo.isGroup && chatInfo.isOnline) {
                val infiniteTransition = rememberInfiniteTransition(label = "online_ring")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "online_angle"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp)
                ) {
                    Canvas(Modifier.size(44.dp)) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF25D366),
                                    Color(0xFF00FFB3),
                                    Color(0xFF25D366)
                                )
                            ),
                            startAngle = angle,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                        )
                    }
                    
                    if (chatInfo.profileImage != null) {
                        // Display profile image
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(chatInfo.profileImage + "?t=${System.currentTimeMillis()}")
                                .crossfade(true)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_report_image)
                                .build(),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showProfilePreview = chatInfo.profileImage },
                            contentScale = ContentScale.Crop,
                            onLoading = { println("[ChatScreen] Loading profile image: ${chatInfo.profileImage}") },
                            onSuccess = { println("[ChatScreen] Successfully loaded profile image: ${chatInfo.profileImage}") },
                            onError = { println("[ChatScreen] Error loading profile image: ${chatInfo.profileImage} - ${it.result.throwable?.message}") }
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = theme.card,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = theme.groupAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                if (chatInfo.profileImage != null) {
                    // Display profile image
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(chatInfo.profileImage + "?t=${System.currentTimeMillis()}")
                            .crossfade(true)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .build(),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, if (chatInfo.isGroup) theme.groupAccent else theme.dmAccent, CircleShape)
                            .clickable { showProfilePreview = chatInfo.profileImage },
                        contentScale = ContentScale.Crop,
                        onLoading = { println("[ChatScreen] Loading profile image: ${chatInfo.profileImage}") },
                        onSuccess = { println("[ChatScreen] Successfully loaded profile image: ${chatInfo.profileImage}") },
                        onError = { println("[ChatScreen] Error loading profile image: ${chatInfo.profileImage} - ${it.result.throwable?.message}") }
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = theme.card,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (chatInfo.isGroup) Icons.Default.Group else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (chatInfo.isGroup) theme.groupAccent else theme.dmAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chatInfo.name, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chatInfo.isGroup) {
                    Text("${chatInfo.memberCount} members", color = theme.textSecondary, fontSize = 12.sp)
                } else {
                    Text(if (chatInfo.isOnline) "Online" else "Offline", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onCallClick) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = theme.groupAccent)
            }
            IconButton(onClick = onVideoClick) {
                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = theme.groupAccent)
            }
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = theme.groupAccent)
            }
        }
    }
    
    // Profile image preview dialog
    if (showProfilePreview != null) {
        Dialog(onDismissRequest = { showProfilePreview = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(showProfilePreview + "?t=${System.currentTimeMillis()}")
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
                    onClick = { showProfilePreview = null },
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

// --- DATE SEPARATOR CHIP ---
@Composable
fun DateSeparatorChip(date: String, theme: KonvoTheme) {
    Surface(
        color = theme.card.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(date, color = theme.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
    }
}

// --- REPLY/QUOTE PREVIEW BUBBLE ---
@Composable
fun ReplyPreviewBubble(replyText: String, theme: KonvoTheme, onCancel: () -> Unit) {
    Surface(
        color = theme.card.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.Reply, contentDescription = null, tint = theme.groupAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(replyText, color = theme.textSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = theme.textSecondary)
            }
        }
    }
}

// Helper sealed class for chat list items
sealed class ChatListUiItem {
    data class DateSeparator(val date: String) : ChatListUiItem()
    data class Message(val message: ChatMessage) : ChatListUiItem()
}

// --- UPDATED MAIN CHATSCREEN COMPOSABLE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    nav: NavController,
    myUid: String,
    otherUid: String,
    chatName: String,
    isGroupChat: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val userId = myUid
    val userName = Firebase.auth.currentUser?.displayName ?: "You"

    fun isValidFirebaseUid(uid: String): Boolean {
        return uid.length >= 20 && uid.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }
    if (!isValidFirebaseUid(myUid) || !isValidFirebaseUid(otherUid)) {
        throw IllegalArgumentException("[ChatScreen] myUid or otherUid is not a valid Firebase UID: myUid=$myUid, otherUid=$otherUid")
    }
    
    // Core chat states
    var messageText by rememberSaveable { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var replyTo by rememberSaveable { mutableStateOf<ChatMessage?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Media and preview states
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf<MessageType?>(null) }
    var pendingCaption by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showProfileImagePreview by remember { mutableStateOf<String?>(null) }
    
    // WhatsApp-style features
    var selectedMessages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf<ChatMessage?>(null) }
    var showMessageOptions by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }
    var showTopBarMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showChatInfo by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showStarredMessages by remember { mutableStateOf(false) }
    var showMediaGallery by remember { mutableStateOf(false) }
    var showVoiceMessagePlayer by remember { mutableStateOf<ChatMessage?>(null) }

    var showSingleMessageMenu by remember { mutableStateOf(false) }
    var showMessageInfoDialog by remember { mutableStateOf(false) }

    // ... after other state declarations inside ChatScreen composable ...
    val audioFilePath = remember { mutableStateOf<String?>(null) }
    val mediaRecorder = remember { mutableStateOf<android.media.MediaRecorder?>(null) }

    fun startRecording() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions((context as android.app.Activity), arrayOf(android.Manifest.permission.RECORD_AUDIO), 0)
            return
        }
        val file = java.io.File(context.cacheDir, "audio_${System.currentTimeMillis()}.3gp")
        audioFilePath.value = file.absolutePath
        mediaRecorder.value = android.media.MediaRecorder().apply {
            setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stopRecording() {
        mediaRecorder.value?.apply {
            stop()
            release()
        }
        mediaRecorder.value = null
        isRecording = false
        // Upload to Firebase Storage and send message
        audioFilePath.value?.let { path ->
            val fileUri = android.net.Uri.fromFile(java.io.File(path))
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val audioRef = storageRef.child("chat_audio/${System.currentTimeMillis()}_${fileUri.lastPathSegment}")
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            audioRef.putFile(fileUri)
                .addOnSuccessListener { taskSnapshot ->
                    audioRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        val msg = hashMapOf(
                            "mediaUrl" to downloadUrl.toString(),
                            "type" to "audio",
                            "senderId" to userId,
                            "senderName" to userName,
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "status" to "sent"
                        )
                        db.collection("chats").document(com.example.konvo.feature.user.get1to1ChatId(myUid, otherUid)).collection("messages")
                            .add(msg)
                    }
                }
        }
    }

    // --- Enhanced Media Pickers for WhatsApp Features ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Make a copy of the URI to ensure we have stable access
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // Create a temp file to store the image
                    val tempFile = java.io.File.createTempFile("image_", ".jpg", context.cacheDir)
                    val outputStream = java.io.FileOutputStream(tempFile)
                    
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    // Convert to URI that we know we have permissions for
                    val fileUri = android.net.Uri.fromFile(tempFile)
                    pendingMedia = fileUri
            pendingMediaType = MessageType.IMAGE
            pendingCaption = ""
                } else {
                    println("[ChatScreen] Failed to open input stream for selected image")
                    android.widget.Toast.makeText(
                        context,
                        "Failed to process selected image",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                println("[ChatScreen] Error processing selected image: ${e.message}")
                android.widget.Toast.makeText(
                    context,
                    "Error processing image: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingMedia = it
            pendingMediaType = MessageType.VIDEO
            pendingCaption = ""
        }
    }
    
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingMedia = it
            pendingMediaType = MessageType.AUDIO
            pendingCaption = ""
        }
    }
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Make a copy of the URI to ensure we have stable access
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // Get the file name
                    val fileName = uri.lastPathSegment ?: "document"
                    
                    // Create a temp file to store the document
                    val tempFile = java.io.File.createTempFile("doc_", "_$fileName", context.cacheDir)
                    val outputStream = java.io.FileOutputStream(tempFile)
                    
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    // Convert to URI that we know we have permissions for
                    val fileUri = android.net.Uri.fromFile(tempFile)
                    pendingMedia = fileUri
            pendingMediaType = MessageType.DOCUMENT
            pendingCaption = ""
                } else {
                    println("[ChatScreen] Failed to open input stream for selected document")
                    android.widget.Toast.makeText(
                        context,
                        "Failed to process selected document",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                println("[ChatScreen] Error processing selected document: ${e.message}")
                android.widget.Toast.makeText(
                    context,
                    "Error processing document: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                pendingMedia = uri
                pendingMediaType = MessageType.IMAGE
                pendingCaption = ""
            }
        }
    }
    
    val videoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                pendingMedia = uri
                pendingMediaType = MessageType.VIDEO
                pendingCaption = ""
            }
        }
    }
    
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            // Handle contact selection
            showContactPicker = true
        }
    }
    var themeIndex by remember { mutableStateOf(0) }
    var animatedThemeEnabled by remember { mutableStateOf(true) }
    var themeLoaded by remember { mutableStateOf(false) }
    
    // Real-time Firestore messages state
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var filteredMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    
    // Current theme
    val currentTheme = konvoThemes[themeIndex]
    
    // Chat info state
    var otherUserProfileImage by remember { mutableStateOf<String?>(null) }
    var isOtherUserOnline by remember { mutableStateOf(false) }
    
    // Search functionality
    LaunchedEffect(searchQuery, messages) {
        filteredMessages = if (searchQuery.isEmpty()) {
            messages
        } else {
            messages.filter { message ->
                message.text.contains(searchQuery, ignoreCase = true) ||
                message.senderName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val chatId = FirestoreRepository.getChatId(myUid, otherUid)
    println("[ChatScreen] myUid=$myUid, otherUid=$otherUid, chatId=$chatId (FirestoreRepository.getChatId)")
    
    // Add listener registration for real-time messaging
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }
    
    // Load other user's profile data
    LaunchedEffect(otherUid) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(otherUid).get().await()
            otherUserProfileImage = userDoc.getString("profileImage")
            isOtherUserOnline = userDoc.getBoolean("isOnline") ?: false
            
            println("[ChatScreen] Loaded other user profile: $otherUid, profileImage=$otherUserProfileImage, isOnline=$isOtherUserOnline")
        } catch (e: Exception) {
            println("[ChatScreen] Error loading other user profile: ${e.message}")
        }
    }
    
    // Set up real-time messaging listener
    LaunchedEffect(chatId) {
        // Remove previous listener if any
        listenerRegistration?.remove()
        
        try {
            // Start listening for messages
            listenerRegistration = FirestoreRepository.listenForMessages(
                chatId = chatId,
                onMessagesUpdate = { messageDataList ->
                    // Process incoming messages
                    val processedMessages = messageDataList.mapNotNull { data ->
                        val id = data["id"] as? String ?: return@mapNotNull null
                        val text = data["text"] as? String ?: ""
                        val timestamp = (data["timestamp"] as? Timestamp)?.seconds?.times(1000) 
                            ?: (data["localTimestamp"] as? Long) ?: System.currentTimeMillis()
                        val senderId = data["senderId"] as? String ?: return@mapNotNull null
                        val senderName = data["senderName"] as? String ?: "Unknown"
                        val status = MessageStatus.fromString(data["status"] as? String)
                        val messageType = MessageType.fromString(data["type"] as? String)
                        val mediaUrl = data["mediaUrl"] as? String
                        val fileName = data["fileName"] as? String
                        val fileSize = data["fileSize"] as? Long
                        val isFromMe = senderId == userId
                        
                        // Parse additional message data for WhatsApp-like features
                        val replyTo = data["replyTo"] as? Map<*, *>
                        val reactions = (data["reactions"] as? Map<*, *>)?.mapNotNull { 
                            (it.key as? String)?.to(it.value as? String ?: "")
                        }?.toMap() ?: emptyMap()
                        val audioDuration = (data["audioDuration"] as? Number)?.toInt()
                        val videoDuration = (data["videoDuration"] as? Number)?.toInt()
                        val location = data["location"] as? Map<*, *>
                        val contact = data["contact"] as? Map<*, *>
                        
                        val isForwarded = data["isForwarded"] as? Boolean ?: false
                        val forwardedFrom = data["forwardedFrom"] as? String
                        val isEdited = data["isEdited"] as? Boolean ?: false
                        val editTimestamp = data["editTimestamp"] as? Long
                        val isStarred = data["isStarred"] as? Boolean ?: false
                        val isDeleted = data["isDeleted"] as? Boolean ?: false
                        val deletedAt = data["deletedAt"] as? Long
                        val deliveredAt = data["deliveredAt"] as? Long
                        val readAt = data["readAt"] as? Long
                        
                        val deletedFor = (data["deletedFor"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        
                        // Construct ChatMessage object
                        ChatMessage(
                            id = id,
                            text = text,
                            timestamp = timestamp,
                            isFromMe = isFromMe,
                            senderName = senderName,
                            isGroupChat = isGroupChat,
                            messageStatus = status,
                            messageType = messageType,
                            mediaUrl = mediaUrl,
                            fileName = fileName,
                            fileSize = fileSize,
                            replyTo = replyTo?.let { replyMap ->
                                ReplyMessage(
                                    id = (replyMap["id"] as? String) ?: "",
                                    text = (replyMap["text"] as? String) ?: "",
                                    senderName = (replyMap["senderName"] as? String) ?: "",
                                    messageType = MessageType.fromString(replyMap["messageType"] as? String)
                                )
                            },
                            reactions = reactions,
                            audioDuration = audioDuration,
                            videoDuration = videoDuration,
                            location = location?.let { locMap ->
                                LocationData(
                                    latitude = (locMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                                    longitude = (locMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                                    address = locMap["address"] as? String
                                )
                            },
                            contact = contact?.let { contactMap ->
                                ContactData(
                                    name = (contactMap["name"] as? String) ?: "",
                                    phoneNumber = (contactMap["phoneNumber"] as? String) ?: "",
                                    email = contactMap["email"] as? String
                                )
                            },
                            isForwarded = isForwarded,
                            forwardedFrom = forwardedFrom,
                            isEdited = isEdited,
                            editTimestamp = editTimestamp,
                            isStarred = isStarred,
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            deliveredAt = deliveredAt,
                            readAt = readAt,
                            deletedFor = deletedFor
                        )
                    }
                    
                    // Update the messages state
                    messages = processedMessages
                    
                    // Mark incoming messages as read
                    scope.launch {
                        try {
                            // Only mark as read if we're the recipient
                            if (messages.any { !it.isFromMe && it.messageStatus != MessageStatus.READ }) {
                                FirestoreRepository.markMessagesAsRead(chatId, userId)
                            }
                        } catch (e: Exception) {
                            println("[ChatScreen] Error marking messages as read: ${e.message}")
                            // Don't show an error to the user as this is a background operation
                        }
                    }
                },
                onError = { error ->
                    println("[ChatScreen] Error listening for messages: ${error.message}")
                    // Could show a snackbar or other error UI here
                }
            )
            
            // Mark existing messages as read when chat is opened
            scope.launch {
                try {
                    FirestoreRepository.markMessagesAsRead(chatId, userId)
                } catch (e: Exception) {
                    println("[ChatScreen] Error marking messages as read on open: ${e.message}")
                    // Show an error to the user
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context, 
                            "Error updating message status", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            println("[ChatScreen] Error setting up message listener: ${e.message}")
            // Show an error to the user
            android.widget.Toast.makeText(
                context, 
                "Error connecting to chat: ${e.message}", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
                    }
                }
    
    // Clean up listener when screen is closed
    DisposableEffect(Unit) {
        onDispose {
            println("[ChatScreen] Cleaning up message listener")
            listenerRegistration?.remove()
        }
    }
    
    // Update online status when entering/leaving chat
    LaunchedEffect(Unit) {
        // Set user as online when entering chat
        if (myUid != null) {
            FirestoreRepository.updateUserOnlineStatus(myUid, true)
        }
        
        // Load theme preferences
        val (savedTheme, savedAnimated) = readThemePrefs(context)
        themeIndex = savedTheme
        animatedThemeEnabled = savedAnimated
        themeLoaded = true
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Set user as offline when leaving chat
            // In a real app, you might want to consider a more sophisticated
            // approach that accounts for app backgrounding/foregrounding
            scope.launch {
                if (myUid != null) {
                    FirestoreRepository.updateUserOnlineStatus(myUid, false)
                }
            }
        }
    }
    
    // Modified message sending logic to handle errors
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        scope.launch {
            try {
                // Send the message - this already includes unreadCount increment in the FirestoreRepository
                FirestoreRepository.sendMessage(userId, otherUid, text)
                
                // Clear reply if there was one
                replyTo = null
                
                // Provide haptic feedback on success
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            } catch (e: Exception) {
                println("[ChatScreen] Error sending message: ${e.message}")
                // Show error to user
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context, 
                        "Failed to send message: ${e.message}", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    if (!themeLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val theme = konvoThemes[themeIndex].copy(animatedBackground = animatedThemeEnabled)
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val chatInfo = ChatInfo(
        chatId = chatId, 
        name = chatName, 
        isGroup = isGroupChat, 
        memberCount = if (isGroupChat) 5 else 0, 
        isOnline = isOtherUserOnline,
        profileImage = otherUserProfileImage
    )
    var hasScrolledInitially by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (!hasScrolledInitially) {
                listState.scrollToItem(messages.size - 1)
                hasScrolledInitially = true
            } else {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    val typingAnimation by rememberInfiniteTransition(label = "typing").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "typing_dots"
    )
    ChatBackground(theme = theme) {
        Column(Modifier.fillMaxSize()) {
            // --- Search Bar UI ---
            if (showSearch) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = theme.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSearch = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = theme.textPrimary)
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.groupAccent,
                                unfocusedBorderColor = theme.textSecondary,
                                focusedTextColor = theme.textPrimary,
                                unfocusedTextColor = theme.textPrimary,
                                cursorColor = theme.groupAccent,
                                focusedPlaceholderColor = theme.textSecondary,
                                unfocusedPlaceholderColor = theme.textSecondary
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = theme.textSecondary)
                            }
                        }
                    }
                }
            }
            ModernChatTopBar(
                chatInfo = chatInfo,
                theme = theme,
                onBackClick = { nav.popBackStack() },
                onMoreClick = { showTopBarMenu = true },
                onSearchClick = { showSearch = true },
                onCallClick = { /* TODO: Call */ },
                onVideoClick = { /* TODO: Video */ },
                onInfoClick = { showChatInfo = true },
                isSelectionMode = isSelectionMode,
                selectedCount = selectedMessages.size,
                onSelectionCancel = {
                    isSelectionMode = false
                    selectedMessages = emptySet()
                },
                onStarSelected = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val star = selectedMessages.any { id -> messages.find { it.id == id }?.isStarred == false }
                    selectedMessages.forEach { msgId ->
                        db.collection("chats").document(chatId).collection("messages").document(msgId)
                            .update("isStarred", star)
                    }
                    isSelectionMode = false
                    selectedMessages = emptySet()
                    // Optionally show a snackbar or toast for feedback
                    scope.launch {
                        val msg = if (star) "Messages starred" else "Messages unstarred"
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDeleteSelected = {
                    showDeleteDialog = true
                },
                onForwardSelected = {
                    // TODO: Implement forward selected
                },
                onSingleMessageMore = {
                    showSingleMessageMenu = true
                }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val chatUiItems = remember(filteredMessages) {
                    val result = mutableListOf<ChatListUiItem>()
                    var lastDate: String? = null
                    
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Kolkata") // Set to Indian Standard Time
                    }
                    
                    for (msg in filteredMessages) {
                        val date = dateFormat.format(Date(msg.timestamp))
                        if (date != lastDate) {
                            result.add(ChatListUiItem.DateSeparator(date))
                            lastDate = date
                        }
                        result.add(ChatListUiItem.Message(msg))
                    }
                    result
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
                ) {
                    var lastSenderFromMe: Boolean? = null
                    items(chatUiItems) { item ->
                        when (item) {
                            is ChatListUiItem.DateSeparator -> DateSeparatorChip(item.date, theme)
                            is ChatListUiItem.Message -> {
                                val msg = item.message
                                // Skip rendering if deleted for this user
                                if (msg.deletedFor.contains(userId)) return@items
                                val fromMe = msg.isFromMe
                                if (msg.isDeleted) {
                                    // Show deleted placeholder
                                    DeletedMessagePlaceholder(theme)
                                } else {
                                    PrettyMessageBubble(
                                        message = msg,
                                        theme = theme,
                                        isGroupChat = isGroupChat,
                                        showAvatar = isGroupChat && !msg.isFromMe,
                                        onLongPress = {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedMessages = setOf(msg.id)
                                            } else {
                                                selectedMessages = selectedMessages + msg.id
                                            }
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        },
                                        onImageClick = { fullScreenImageUrl = it },
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedMessages.contains(msg.id),
                                        onSelectionToggle = {
                                            if (selectedMessages.contains(msg.id)) {
                                                selectedMessages = selectedMessages - msg.id
                                                if (selectedMessages.isEmpty()) isSelectionMode = false
                                            } else {
                                                selectedMessages = selectedMessages + msg.id
                                            }
                                        }
                                    )
                                }
                                lastSenderFromMe = fromMe
                            }
                        }
                    }
                    if (isTyping) {
                        item {
                            TypingIndicator(theme = theme, animation = typingAnimation)
                        }
                    }
                }
                // --- Jump to Bottom FAB (refined logic: only show if not at bottom) ---
                val showJumpToBottom by remember {
                    derivedStateOf {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex <= 0) return@derivedStateOf false
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@derivedStateOf false
                        // Show if the last visible item is NOT the last message
                        val lastVisible = visibleItems.last().index
                        lastVisible < lastIndex
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showJumpToBottom,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (messages.isNotEmpty()) {
                                scope.launch { listState.animateScrollToItem(messages.size - 1) }
                            }
                        },
                        containerColor = theme.groupAccent,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Jump to bottom")
                    }
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Column(Modifier.fillMaxWidth()) {
                    if (replyTo != null) {
                        ReplyPreviewBubble(replyText = replyTo!!.text, theme = theme, onCancel = { replyTo = null })
                    }
                    FloatingInputBar(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendClick = {
                            if (messageText.trim().isNotEmpty()) {
                                val textToSend = messageText.trim()
                                messageText = "" // Clear input immediately for responsive UI
                                sendMessage(textToSend)
                            }
                        },
                        onAttachmentClick = { showAttachmentMenu = true },
                        onEmojiClick = { showEmojiPicker = !showEmojiPicker },
                        onVoiceRecordStart = { startRecording() },
                        onVoiceRecordStop = { stopRecording() },
                        isRecording = isRecording,
                        recordingDuration = recordingDuration,
                        replyTo = replyTo,
                        onReplyCancel = { replyTo = null },
                        theme = theme,
                        focusRequester = focusRequester
                    )
                }
            }
        }
        if (showAttachmentMenu) {
            AttachmentMenu(
                onDismiss = { showAttachmentMenu = false },
                onCameraClick = { 
                    try {
                    // Create a temporary file for camera photo
                    val photoFile = java.io.File.createTempFile("camera_photo", ".jpg", context.cacheDir)
                    val photoUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraPhotoUri = photoUri
                    cameraLauncher.launch(photoUri)
                    } catch (e: Exception) {
                        println("[ChatScreen] Error launching camera: ${e.message}")
                        android.widget.Toast.makeText(
                            context,
                            "Failed to launch camera: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showAttachmentMenu = false
                },
                onGalleryClick = { 
                    try {
                    imagePickerLauncher.launch("image/*")
                    } catch (e: Exception) {
                        println("[ChatScreen] Error launching image picker: ${e.message}")
                        android.widget.Toast.makeText(
                            context,
                            "Failed to open gallery: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showAttachmentMenu = false
                },
                onDocumentClick = { 
                    try {
                    documentPickerLauncher.launch("*/*")
                    } catch (e: Exception) {
                        println("[ChatScreen] Error launching document picker: ${e.message}")
                        android.widget.Toast.makeText(
                            context,
                            "Failed to open document picker: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showAttachmentMenu = false
                },
                theme = theme
            )
        }
        if (showEmojiPicker) {
            EmojiPicker(
                onEmojiSelected = { emoji ->
                    messageText += emoji
                    showEmojiPicker = false
                },
                onDismiss = { showEmojiPicker = false },
                theme = theme
            )
        }
        // --- Media Preview Dialog ---
        if (pendingMedia != null && pendingMediaType != null) {
            Dialog(onDismissRequest = { pendingMedia = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (pendingMediaType == MessageType.IMAGE) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(pendingMedia)
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Description, contentDescription = "Document", tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Text(pendingMedia?.lastPathSegment ?: "Document", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pendingCaption,
                            onValueChange = { pendingCaption = it },
                            label = { Text("Add a caption...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { pendingMedia = null }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                val currentMedia = pendingMedia
                                if (currentMedia == null) {
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Error: Media is missing", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                
                                if (pendingMediaType == MessageType.IMAGE) {
                                    scope.launch {
                                        try {
                                        FirestoreRepository.sendImageMessage(
                                            fromId = userId,
                                            toId = otherUid,
                                                imageUri = currentMedia,
                                            caption = pendingCaption
                                        )
                                        } catch (e: Exception) {
                                            println("[ChatScreen] Error sending image: ${e.message}")
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context, 
                                                    "Failed to send image: ${e.message}", 
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } else if (pendingMediaType == MessageType.DOCUMENT) {
                                    scope.launch {
                                        try {
                                        FirestoreRepository.sendDocumentMessage(
                                            fromId = userId,
                                            toId = otherUid,
                                                docUri = currentMedia,
                                                fileName = currentMedia.lastPathSegment,
                                            caption = pendingCaption
                                        )
                                        } catch (e: Exception) {
                                            println("[ChatScreen] Error sending document: ${e.message}")
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context, 
                                                    "Failed to send document: ${e.message}", 
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    }
                                }
                                pendingMedia = null
                                pendingCaption = ""
                            }) { Text("Send") }
                        }
                    }
                }
            }
        }
        // --- Full-Screen Image Viewer Dialog ---
        if (fullScreenImageUrl != null) {
            Dialog(onDismissRequest = { fullScreenImageUrl = null }) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(fullScreenImageUrl + "?t=${System.currentTimeMillis()}")
                            .crossfade(true)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { fullScreenImageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
        // --- Show MessageOptionsMenu when a message is selected ---
        if (selectedMessageForOptions != null) {
            MessageOptionsMenu(
                message = selectedMessageForOptions!!,
                onReply = {
                    replyTo = selectedMessageForOptions
                    selectedMessageForOptions = null
                },
                onForward = {
                    // TODO: Implement forward logic (showForwardDialog = selectedMessageForOptions)
                    selectedMessageForOptions = null
                },
                onCopy = {
                    // Todo
                },
                onStar = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val msgId = selectedMessageForOptions!!.id
                    val chatRef = db.collection("chats").document(chatId).collection("messages").document(msgId)
                    val newStar = !selectedMessageForOptions!!.isStarred
                    chatRef.update("isStarred", newStar)
                    // Optionally show a snackbar or toast for feedback
                    scope.launch {
                        val msg = if (newStar) "Message starred" else "Message unstarred"
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    selectedMessageForOptions = null
                },
                onDelete = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val msgId = selectedMessageForOptions!!.id
                    val chatRef = db.collection("chats").document(chatId).collection("messages").document(msgId)
                    chatRef.update("isDeleted", true, "deletedAt", System.currentTimeMillis())
                    selectedMessageForOptions = null
                },
                onEdit = {
                    // TODO: Implement edit logic (showEditMessage = selectedMessageForOptions)
                    selectedMessageForOptions = null
                },
                onDismiss = { selectedMessageForOptions = null },
                theme = theme
            )
        }
        // --- Multi-Select Actions ---
        // (Block removed: duplicate ModernChatTopBar and selection Row)

        // --- Top Bar Menu Logic ---
        DropdownMenu(
            expanded = showTopBarMenu,
            onDismissRequest = { showTopBarMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Archive Chat") },
                onClick = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("chats").document(chatId).update("archived", true)
                    showTopBarMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Mute Notifications") },
                onClick = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val muteUntil = System.currentTimeMillis() + 8 * 60 * 60 * 1000
                    db.collection("chats").document(chatId).update("mutedUntil", muteUntil)
                    showTopBarMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Clear Chat") },
                onClick = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("chats").document(chatId).collection("messages")
                        .get().addOnSuccessListener { snapshot ->
                            snapshot.documents.forEach { doc ->
                                doc.reference.update("isDeleted", true, "deletedAt", System.currentTimeMillis())
                            }
                        }
                    showTopBarMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Block User") },
                onClick = {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val currentUserId = userId
                    db.collection("users").document(currentUserId)
                        .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(chatInfo.chatId))
                    showTopBarMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Starred Messages") },
                onClick = {
                    showStarredMessages = true
                    showTopBarMenu = false
                }
            )
        }
        // --- Forward Message Dialog ---
        if (showForwardDialog != null) {
            val availableChats = listOf("Chat 1", "Chat 2", "Chat 3")
            var selectedChats by remember { mutableStateOf(setOf<String>()) }
            AlertDialog(
                onDismissRequest = { showForwardDialog = null },
                title = { Text("Forward to...") },
                text = {
                    Column {
                        availableChats.forEach { chatName ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selectedChats = if (selectedChats.contains(chatName)) selectedChats - chatName else selectedChats + chatName
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedChats.contains(chatName),
                                    onCheckedChange = { checked ->
                                        selectedChats = if (checked) selectedChats + chatName else selectedChats - chatName
                                    }
                                )
                                Text(chatName)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val msg = showForwardDialog!!
                        selectedChats.forEach { chatName ->
                            val chatIdToForward = chatName
                            val forwardMsg = msg.copy(isForwarded = true, forwardedFrom = userName, timestamp = System.currentTimeMillis())
                            db.collection("chats").document(chatIdToForward).collection("messages").add(forwardMsg)
                        }
                        showForwardDialog = null
                    }) { Text("Forward") }
                },
                dismissButton = {
                    TextButton(onClick = { showForwardDialog = null }) { Text("Cancel") }
                }
            )
        }
        // --- Edit Message Dialog ---
        if (showEditMessage != null) {
            var editText by remember { mutableStateOf(showEditMessage!!.text) }
            AlertDialog(
                onDismissRequest = { showEditMessage = null },
                title = { Text("Edit Message") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        db.collection("chats").document(chatId).collection("messages").document(showEditMessage!!.id)
                            .update("text", editText, "isEdited", true, "editTimestamp", System.currentTimeMillis())
                        showEditMessage = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditMessage = null }) { Text("Cancel") }
                }
            )
        }
        // --- Single Message Options Menu ---
        if (showSingleMessageMenu && selectedMessages.size == 1) {
            val selectedMsg = messages.find { it.id == selectedMessages.first() }
            if (selectedMsg != null) {
                SingleMessageOptionsMenu(
                    message = selectedMsg,
                    onEdit = { showEditMessage = selectedMsg },
                    onPin = { /* TODO: Pin logic */ },
                    onInfo = { showMessageInfoDialog = true },
                    onDismiss = { showSingleMessageMenu = false },
                    theme = theme
                )
            }
        }
        if (showMessageInfoDialog && selectedMessages.size == 1) {
            val selectedMsg = messages.find { it.id == selectedMessages.first() }
            if (selectedMsg != null) {
                MessageInfoDialog(
                    message = selectedMsg,
                    onDismiss = { showMessageInfoDialog = false },
                    theme = theme
                )
            }
        }
        if (showDeleteDialog && selectedMessages.isNotEmpty()) {
            val canDeleteForEveryone = selectedMessages
                .mapNotNull { id -> messages.find { it.id == id } }
                .all { it.isFromMe }
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete message(s)?", color = theme.textPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Do you want to delete for yourself or for everyone?",
                            color = theme.textSecondary,
                            fontSize = 15.sp
                        )
                        if (canDeleteForEveryone) {
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "'Delete for everyone' will remove the message for all participants.",
                                    color = Color.Red,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = {
                                // Delete for me
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                selectedMessages.forEach { msgId ->
                                    val msgRef = db.collection("chats").document(chatId).collection("messages").document(msgId)
                                    msgRef.update("deletedFor", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                }
                                isSelectionMode = false
                                selectedMessages = emptySet()
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.card)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete for me", color = theme.textPrimary)
                        }
                        if (canDeleteForEveryone) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    // Delete for everyone
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    selectedMessages.forEach { msgId ->
                                        val msgRef = db.collection("chats").document(chatId).collection("messages").document(msgId)
                                        msgRef.update("isDeleted", true, "deletedAt", System.currentTimeMillis())
                                    }
                                    isSelectionMode = false
                                    selectedMessages = emptySet()
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Delete for everyone", color = Color.White)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                },
                containerColor = theme.card
            )
        }
        if (showStarredMessages) {
            Dialog(onDismissRequest = { showStarredMessages = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.card,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Starred Messages", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        val starred = messages.filter { it.isStarred && !it.isDeleted && !it.deletedFor.contains(userId) }
                        if (starred.isEmpty()) {
                            Text("No starred messages.", color = theme.textSecondary)
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(starred) { msg ->
                                    PrettyMessageBubble(
                                        message = msg,
                                        theme = theme,
                                        isGroupChat = isGroupChat,
                                        showAvatar = isGroupChat && !msg.isFromMe,
                                        onLongPress = {},
                                        onImageClick = {},
                                        isSelectionMode = false,
                                        isSelected = false,
                                        onSelectionToggle = {}
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showStarredMessages = false }, modifier = Modifier.align(Alignment.End)) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentPreview(
    fileName: String,
    fileSize: Long?,
    theme: KonvoTheme,
    textColor: Color,
    mediaUrl: String? = null
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .clickable {
                mediaUrl?.let { url ->
                try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = url.toUri()
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                    // Fallback: try to open with browser
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(browserIntent)
                        } catch (e2: Exception) {
                            // Show toast or handle error
                        }
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = "Document",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                fileName,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            fileSize?.let { size ->
                Text(
                    formatFileSize(size),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = "Open Document",
            tint = textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, tint: Color) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule
        MessageStatus.SENT -> Icons.Default.Done
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Default.DoneAll
    }
    
    val alpha = when (status) {
        MessageStatus.SENDING -> 0.5f
        MessageStatus.SENT -> 0.7f
        MessageStatus.DELIVERED -> 0.9f
        MessageStatus.READ -> 1f
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint.copy(alpha = alpha),
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun TypingIndicator(
    theme: KonvoTheme,
    animation: Float
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(start = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.messageBubbleOther,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(theme.textSecondary.copy(alpha = 0.6f))
                            .scale(
                                lerp(
                                    0.8f,
                                    1.2f,
                                    (animation + index * 0.2f) % 1f
                                )
                            )
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputSection(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    theme: KonvoTheme,
    focusRequester: FocusRequester
) {
    Surface(
        color = theme.card,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment button
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attachment",
                    tint = theme.textSecondary
                )
            }
            
            // Emoji button
            IconButton(
                onClick = onEmojiClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = theme.textSecondary
                )
            }
            
            // Message input
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    cursorColor = theme.groupAccent,
                    focusedPlaceholderColor = theme.textSecondary,
                    unfocusedPlaceholderColor = theme.textSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendClick() }
                ),
                maxLines = 4
            )
            
            // Send button
            IconButton(
                onClick = onSendClick,
                modifier = Modifier.size(40.dp),
                enabled = messageText.trim().isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageText.trim().isNotEmpty()) theme.groupAccent else theme.textSecondary
                )
            }
        }
    }
}

@Composable
fun AttachmentMenu(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDocumentClick: () -> Unit,
    theme: KonvoTheme
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.card
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                AttachmentOption(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera",
                    onClick = {
                        onCameraClick()
                        onDismiss()
                    },
                    theme = theme
                )
                
                AttachmentOption(
                    icon = Icons.Default.Photo,
                    label = "Gallery",
                    onClick = {
                        onGalleryClick()
                        onDismiss()
                    },
                    theme = theme
                )
                
                AttachmentOption(
                    icon = Icons.Default.Description,
                    label = "Document",
                    onClick = {
                        onDocumentClick()
                        onDismiss()
                    },
                    theme = theme
                )
            }
        }
    }
}

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    theme: KonvoTheme
) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
        "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
        "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
        "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
        "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
        "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠",
        "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨",
        "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫", "🤥"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .heightIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.card
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Emojis",
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clickable { onEmojiSelected(emoji) }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ModernTextBubble(
    message: String,
    time: String,
    isFromMe: Boolean,
    status: MessageStatus? = null,
    isEdited: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isFromMe) Color(0xFF4F8CFF) else Color(0xFFF0F0F0)
    val textColor = if (isFromMe) Color.White else Color(0xFF222222)
    val alignment = if (isFromMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bubbleColor,
            shadowElevation = 3.dp,
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f)
                .defaultMinSize(minWidth = 48.dp)
        ) {
            Box(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message,
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = time,
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        if (isEdited) {
                            Spacer(Modifier.width(4.dp))
                            Text("(edited)", color = textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                        if (isFromMe && status != null) {
                            Spacer(Modifier.width(4.dp))
                            MessageStatusIcon(status, textColor.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedMessagePlaceholder(theme: KonvoTheme) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = theme.card,
            shadowElevation = 1.dp
        ) {
            Text(
                text = "This message was deleted",
                color = theme.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}