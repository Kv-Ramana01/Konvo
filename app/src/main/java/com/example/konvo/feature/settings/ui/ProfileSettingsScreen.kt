package com.example.konvo.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.konvo.ui.theme.KonvoBlue
import com.example.konvo.ui.theme.KonvoBlueDark
import com.example.konvo.ui.theme.konvoThemes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import com.example.konvo.util.themeDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.example.konvo.R
import android.os.Environment
import java.io.File
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import android.Manifest
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import android.content.pm.PackageManager
import java.io.FileOutputStream

// Custom CropImageContract using UCrop
class CropImageContract : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        
        // We use the standard MediaStore crop intent which works on most Android devices
        return Intent("com.android.camera.action.CROP")
            .setDataAndType(input, "image/*")
            .putExtra("crop", "true")
            .putExtra("aspectX", 1)
            .putExtra("aspectY", 1)
            .putExtra("scale", true)
            .putExtra("outputX", 512)
            .putExtra("outputY", 512)
            .putExtra("return-data", false)
            .putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
            .putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        // The cropped image URI is in the output we specified
        return intent?.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
    }
}

// Custom CropImageContract that handles more devices
class ImprovedCropImageContract : ActivityResultContract<Uri, Uri?>() {
    // Store application context as a field
    private lateinit var appContext: Context
    private lateinit var outputFile: File
    
    override fun createIntent(context: Context, input: Uri): Intent {
        println("[ImprovedCropImageContract] Creating crop intent for URI: $input")
        // Save the context for later use
        this.appContext = context.applicationContext
        
        // Create a content:// URI for the cropped image output
        outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destinationUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
        println("[ImprovedCropImageContract] Created destination URI: $destinationUri")
        
        try {
            // Create the crop intent
            val intent = Intent("com.android.camera.action.CROP")
                .setDataAndType(input, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("crop", "true")
                .putExtra("aspectX", 1)
                .putExtra("aspectY", 1)
                .putExtra("scale", true)
                .putExtra("outputX", 512)
                .putExtra("outputY", 512)
                .putExtra("return-data", false)
                .putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
                .putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            
            // Grant permissions for all available apps that can handle this intent
            val resInfoList = context.packageManager.queryIntentActivities(
                intent, 
                PackageManager.MATCH_DEFAULT_ONLY
            )
            
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    destinationUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                println("[ImprovedCropImageContract] Granted permissions to: $packageName")
            }
            
            // Check if this intent can be handled
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            
            if (resolveInfo != null) {
                println("[ImprovedCropImageContract] Found activity to handle crop: ${resolveInfo.activityInfo.packageName}")
                return intent
            } else {
                println("[ImprovedCropImageContract] No crop activity found, using fallback")
                // Fallback: Just copy the image as-is if no crop activity is available
                try {
                    val inputStream = context.contentResolver.openInputStream(input)
                    if (inputStream != null) {
                        val outputStream = FileOutputStream(outputFile)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()
                        println("[ImprovedCropImageContract] Copied image as fallback")
                    }
                } catch (e: Exception) {
                    println("[ImprovedCropImageContract] Fallback copy failed: ${e.message}")
                }
                // Return an intent that will just return the original URI
                return Intent().putExtra("output_uri", input.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("[ImprovedCropImageContract] Error creating crop intent: ${e.message}")
            // In case of exception, create an intent that will just return the original URI
            return Intent().putExtra("output_uri", input.toString())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        println("[ImprovedCropImageContract] Parsing result, resultCode: $resultCode")
        if (resultCode != Activity.RESULT_OK) {
            println("[ImprovedCropImageContract] Result not OK")
            return null
        }
        
        try {
            // First try to get the destinationUri from our extras
            val outputUriStr = intent?.getStringExtra("output_uri")
            if (outputUriStr != null) {
                println("[ImprovedCropImageContract] Found output_uri in extras: $outputUriStr")
                return Uri.parse(outputUriStr)
            }
            
            // If the app used our output URI, check if file exists and has content
            if (::outputFile.isInitialized && outputFile.exists() && outputFile.length() > 0) {
                println("[ImprovedCropImageContract] Found valid output file: ${outputFile.absolutePath}, size: ${outputFile.length()}")
                return FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    outputFile
                )
            }
            
            // Fallback: search for any recently created crop file
            val cacheDir = File(appContext.cacheDir.path)
            val cropFile = cacheDir.listFiles()?.filter { 
                it.name.startsWith("cropped_") && it.length() > 0 && 
                System.currentTimeMillis() - it.lastModified() < 30000 // Less than 30 seconds old
            }?.maxByOrNull { it.lastModified() }
            
            if (cropFile != null) {
                println("[ImprovedCropImageContract] Found fallback crop file: ${cropFile.absolutePath}")
                return FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    cropFile
                )
            }
            
            println("[ImprovedCropImageContract] No valid output found")
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            println("[ImprovedCropImageContract] Error parsing result: ${e.message}")
            return null
        }
    }
}

// DataStore keys
private val THEME_INDEX_KEY = intPreferencesKey("theme_index")
private val ANIMATED_THEME_KEY = booleanPreferencesKey("animated_theme_enabled")

// Helper class to handle profile operations
class ProfileManager(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onError: (String) -> Unit,
    private val onSuccess: (String) -> Unit,
    private val onSavingChange: (Boolean) -> Unit,
    private val updateName: (String) -> Unit,
    private val updateUsername: (String) -> Unit,
    private val updateOriginalUsername: (String) -> Unit,
    private val updateBio: (String) -> Unit,
    private val updateProfileImageUrl: (String?) -> Unit,
    private val updateUsernameAvailable: (Boolean?) -> Unit
) {
    private val uid = Firebase.auth.currentUser?.uid ?: ""
    private var lastSaveTime = 0L
    private val saveDebounceTime = 800L // 800ms debounce time for auto-save
    private var saveJob: kotlinx.coroutines.Job? = null
    
    suspend fun saveThemePrefs(themeIndex: Int, animated: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_INDEX_KEY] = themeIndex
            prefs[ANIMATED_THEME_KEY] = animated
        }
    }
    
    suspend fun loadUserData() {
        if (uid.isEmpty()) {
            onError("User not logged in")
            return
        }
        
        try {
            // Load profile data from Firestore
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(uid).get().await()
            updateName(doc.getString("name") ?: "")
            val username = doc.getString("username") ?: ""
            updateUsername(username)
            updateOriginalUsername(username) // Store original username
            updateBio(doc.getString("bio") ?: "")
            
            // Add timestamp to profile image URL to force refresh
            val profileImageUrl = doc.getString("profileImage")
            if (profileImageUrl != null) {
                val urlWithTimestamp = if (profileImageUrl.contains("?")) {
                    // URL already has parameters, append timestamp
                    "$profileImageUrl&t=${System.currentTimeMillis()}"
                } else {
                    // URL has no parameters, add timestamp
                    "$profileImageUrl?t=${System.currentTimeMillis()}"
                }
                println("[ProfileManager] Adding timestamp to profile image URL: $urlWithTimestamp")
                updateProfileImageUrl(urlWithTimestamp)
            } else {
                updateProfileImageUrl(null)
            }
        } catch (e: Exception) {
            onError("Failed to load profile: ${e.message}")
        }
    }
    
    // Method to force clear the Firebase Storage cache
    suspend fun forceClearProfileImageCache() {
        if (uid.isEmpty()) {
            onError("User not logged in")
            return
        }
        
        try {
            println("[ProfileManager] Force clearing profile image cache")
            val storage = FirebaseStorage.getInstance()
            
            // List all profile images for this user
            val listResult = storage.reference.child("profile_images").listAll().await()
            
            // Delete all profile images for this user
            var deletedCount = 0
            for (item in listResult.items) {
                if (item.name.startsWith(uid)) {
                    println("[ProfileManager] Deleting profile image: ${item.name}")
                    item.delete().await()
                    deletedCount++
                }
            }
            
            println("[ProfileManager] Deleted $deletedCount profile images from storage")
            
            // Now reload the user data
            loadUserData()
        } catch (e: Exception) {
            println("[ProfileManager] Error clearing profile image cache: ${e.message}")
            onError("Failed to clear image cache: ${e.message}")
        }
    }
    
    suspend fun loadThemePreferences(): Pair<Int, Boolean> {
        try {
            // Load theme preferences from DataStore
            val prefs = context.themeDataStore.data.first()
            val themeIndex = prefs[THEME_INDEX_KEY] ?: 0
            val animated = prefs[ANIMATED_THEME_KEY] ?: true
            return Pair(themeIndex, animated)
        } catch (e: Exception) {
            onError("Failed to load theme preferences: ${e.message}")
            return Pair(0, true)
        }
    }
    
    fun checkUsernameAvailability(
        username: String, 
        originalUsername: String,
        name: String,
        bio: String,
        profileImageUrl: String?,
        profileImageUri: Uri?
    ) {
        // Don't check if username hasn't changed from original
        if (username.isBlank() || username == originalUsername) {
            updateUsernameAvailable(true)
            return
        }
        
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val query = db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()
                
                val isAvailable = query.isEmpty
                updateUsernameAvailable(isAvailable)
                
                // If username is available, trigger save
                if (isAvailable) {
                    triggerSave(
                        name = name,
                        username = username,
                        bio = bio,
                        originalUsername = originalUsername,
                        profileImageUrl = profileImageUrl,
                        profileImageUri = profileImageUri,
                        usernameAvailable = true
                    )
                }
            } catch (e: Exception) {
                onError("Failed to check username: ${e.message}")
                updateUsernameAvailable(null)
            }
        }
    }
    
    // Debounced save function - cancels previous save job if still pending
    fun triggerSave(
        name: String,
        username: String,
        bio: String,
        originalUsername: String,
        profileImageUrl: String?,
        profileImageUri: Uri?,
        usernameAvailable: Boolean?
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Cancel any pending save job
        saveJob?.cancel()
        
        // Check if we're just updating the profile picture with no username change
        val isJustProfilePicUpdate = username == originalUsername && profileImageUri != null
        
        // If we're just updating the profile pic with the same username, we can consider it available
        val effectiveUsernameAvailable = if (isJustProfilePicUpdate) true else usernameAvailable
        
        // Only proceed if we've waited long enough since last save
        if (currentTime - lastSaveTime < saveDebounceTime) {
            // Schedule a new job with delay
            saveJob = scope.launch {
                delay(saveDebounceTime)
                if (name.isNotBlank() && username.isNotBlank() && (effectiveUsernameAvailable != false)) {
                    saveProfile(
                        name = name,
                        username = username,
                        bio = bio,
                        originalUsername = originalUsername,
                        profileImageUrl = profileImageUrl,
                        profileImageUri = profileImageUri,
                        usernameAvailable = effectiveUsernameAvailable
                    )
                }
            }
        } else {
            // It's been long enough since last save, save immediately
            scope.launch {
                if (name.isNotBlank() && username.isNotBlank() && (effectiveUsernameAvailable != false)) {
                    saveProfile(
                        name = name,
                        username = username,
                        bio = bio,
                        originalUsername = originalUsername,
                        profileImageUrl = profileImageUrl,
                        profileImageUri = profileImageUri,
                        usernameAvailable = effectiveUsernameAvailable
                    )
                }
            }
        }
        
        // Update last save time
        lastSaveTime = currentTime
    }
    
    suspend fun saveProfile(
        name: String,
        username: String,
        bio: String,
        originalUsername: String,
        profileImageUrl: String?,
        profileImageUri: Uri?,
        usernameAvailable: Boolean?
    ) {
        if (uid.isEmpty() || name.isBlank() || username.isBlank()) {
            onError("Please enter your name and username")
            return
        }

        // Only check username availability if it has changed from the original
        if (username != originalUsername && usernameAvailable != true) {
            onError("Please choose an available username")
            return
        }

        onSavingChange(true)
        
        try {
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            
            println("[ProfileManager] Starting profile save. Has new image: ${profileImageUri != null}")
            
            // If there's a new profile image, upload it first
            var updatedProfileUrl = profileImageUrl
            if (profileImageUri != null) {
                println("[ProfileManager] Uploading new profile image")
                // Add a unique timestamp to avoid any caching issues with Firebase Storage
                val timestamp = System.currentTimeMillis()
                val imageRef = storage.reference.child("profile_images/${uid}_${timestamp}")
                
                // Delete any existing image first to avoid caching issues
                try {
                    // Find and delete any existing profile images for this user
                    val listResult = storage.reference.child("profile_images").listAll().await()
                    listResult.items.forEach { item ->
                        if (item.name.startsWith(uid)) {
                            println("[ProfileManager] Deleting old profile image: ${item.name}")
                            item.delete().await()
                        }
                    }
                    println("[ProfileManager] Deleted existing profile images")
                } catch (e: Exception) {
                    // It's okay if there was no existing image to delete
                    println("[ProfileManager] Error while cleaning up old images: ${e.message}")
                }
                
                // Upload the new image
                println("[ProfileManager] Starting image upload to ${imageRef.path}")
                val uploadTask = imageRef.putFile(profileImageUri).await()
                println("[ProfileManager] Image upload complete: ${uploadTask.metadata?.path}")
                
                // Get the download URL with a cache-busting parameter
                updatedProfileUrl = imageRef.downloadUrl.await().toString() + "?t=${timestamp}"
                println("[ProfileManager] Got download URL with cache-busting: $updatedProfileUrl")
                
                // Update the profile image URL immediately in the UI
                updateProfileImageUrl(updatedProfileUrl)
            }
            
            // First get the existing profile to compare values
            val userDoc = db.collection("users").document(uid).get().await()
            val oldUsername = userDoc.getString("username") ?: ""
            val oldName = userDoc.getString("name") ?: ""
            val oldImageUrl = userDoc.getString("profileImage")
            
            println("[ProfileManager] Current profile - Name: $oldName, Username: $oldUsername, ImageUrl: $oldImageUrl")
            println("[ProfileManager] New profile - Name: $name, Username: $username, ImageUrl: $updatedProfileUrl")
            
            // Update the profile data
            val profileUpdates = mutableMapOf<String, Any>(
                "name" to name,
                "username" to username,
                "bio" to bio
            )
            
            if (updatedProfileUrl != null) {
                profileUpdates["profileImage"] = updatedProfileUrl
                println("[ProfileManager] Adding profileImage to updates: $updatedProfileUrl")
            }
            
            // Update user document
            println("[ProfileManager] Updating Firestore document with: $profileUpdates")
            val updateTask = db.collection("users").document(uid).update(profileUpdates).await()
            println("[ProfileManager] Firestore update completed")
            
            // Verify the update by reading the document again
            val updatedDoc = db.collection("users").document(uid).get().await()
            val verifiedImageUrl = updatedDoc.getString("profileImage")
            println("[ProfileManager] Verified profile image URL: $verifiedImageUrl")
            
            // If the profile image URL wasn't updated properly, try setting it explicitly
            if (updatedProfileUrl != null && verifiedImageUrl != updatedProfileUrl) {
                println("[ProfileManager] Profile image URL mismatch, trying explicit update")
                db.collection("users").document(uid).update("profileImage", updatedProfileUrl).await()
                
                // Double-check again
                val finalCheck = db.collection("users").document(uid).get().await()
                println("[ProfileManager] Final profile image check: ${finalCheck.getString("profileImage")}")
            }
            
            // Update name/username in all existing chats if they changed
            if (name != oldName || username != oldUsername || updatedProfileUrl != oldImageUrl) {
                println("[ProfileSettings] Updating chats with new user data: $name, $username, $updatedProfileUrl")
                
                // First try with collectionGroup query (more efficient)
                try {
                    // Update all chats where this user is referenced as the other user
                    val chatsQuery = db.collectionGroup("chats")
                        .whereEqualTo("otherUserId", uid)
                        .get()
                        .await()
                    
                    for (chatDoc in chatsQuery.documents) {
                        val updates = mutableMapOf<String, Any>(
                            "userName" to name,
                            "userUsername" to username
                        )
                        
                        // Add profile image if available
                        if (updatedProfileUrl != null) {
                            updates["profileImage"] = updatedProfileUrl
                        }
                        
                        chatDoc.reference.update(updates)
                    }
                } catch (e: Exception) {
                    // If collectionGroup query fails, fall back to manual update
                    println("[ProfileSettings] CollectionGroup query failed: ${e.message}")
                    println("[ProfileSettings] Falling back to manual chat update")
                    
                    // Get all users
                    val usersQuery = db.collection("users").get().await()
                    
                    // For each user, check their chats collection for references to this user
                    for (userDoc in usersQuery.documents) {
                        if (userDoc.id == uid) continue // Skip self
                        
                        val userChatsQuery = db.collection("users").document(userDoc.id)
                            .collection("chats")
                            .whereEqualTo("otherUserId", uid)
                            .get()
                            .await()
                        
                        for (chatDoc in userChatsQuery.documents) {
                            val updates = mutableMapOf<String, Any>(
                                "userName" to name,
                                "userUsername" to username
                            )
                            
                            // Add profile image if available
                            if (updatedProfileUrl != null) {
                                updates["profileImage"] = updatedProfileUrl
                            }
                            
                            chatDoc.reference.update(updates)
                        }
                    }
                }
                
                // Also update message senderName in any messages sent by this user
                try {
                    // Get all chats
                    val allChatsQuery = db.collection("chats").get().await()
                    
                    for (chatDoc in allChatsQuery.documents) {
                        // Get messages sent by this user
                        val messagesQuery = db.collection("chats").document(chatDoc.id)
                            .collection("messages")
                            .whereEqualTo("senderId", uid)
                            .get()
                            .await()
                        
                        // Update sender name in each message
                        for (messageDoc in messagesQuery.documents) {
                            messageDoc.reference.update("senderName", name)
                        }
                    }
                } catch (e: Exception) {
                    println("[ProfileSettings] Error updating message sender names: ${e.message}")
                    // Non-critical, don't fail the entire operation
                }
            }
            
            // Store original username for future comparisons
            updateOriginalUsername(username)
            
            onSuccess("Profile updated")
            
            // Auto-hide success message after 2 seconds and refresh data
            scope.launch {
                delay(2000)
                onSuccess("")
                
                // Reload the user data to ensure UI is in sync with database
                try {
                    println("[ProfileManager] Reloading user data after save")
                    loadUserData()
                    println("[ProfileManager] User data reload complete")
                } catch (e: Exception) {
                    println("[ProfileManager] Error reloading user data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            onError("Failed to update profile: ${e.message}")
        } finally {
            onSavingChange(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    onBackPressed: () -> Unit = {}
) {
    // State variables for profile information
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Theme preferences
    var currentThemeIndex by remember { mutableStateOf(0) } // Default value until loaded
    var animatedThemeEnabled by remember { mutableStateOf(true) }
    val theme = konvoThemes[currentThemeIndex]
    
    // User data
    var name by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") } // Store original username to check if changed
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    
    // Auto-saving state
    var isSaving by remember { mutableStateOf(false) }
    var lastSaveTime by remember { mutableStateOf(0L) }
    val saveDebounceTime = 1000L // 1 second debounce
    
    // Theme selection dialog
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // UI state
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Image picker states
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerOptions by remember { mutableStateOf(false) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showProfileImagePreview by remember { mutableStateOf<String?>(null) }
    
    // Create the profile manager
    val profileManager = remember {
        ProfileManager(
            context = context,
            scope = scope,
            onError = { error = it },
            onSuccess = { success = it },
            onSavingChange = { isSaving = it },
            updateName = { name = it },
            updateUsername = { username = it },
            updateOriginalUsername = { originalUsername = it },
            updateBio = { bio = it },
            updateProfileImageUrl = { profileImageUrl = it },
            updateUsernameAvailable = { usernameAvailable = it }
        )
    }
    
    // Image cropping functionality
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ImprovedCropImageContract()
    ) { croppedUri: Uri? ->
        println("[ProfileSettings] Received cropped image URI: $croppedUri")
        if (croppedUri != null) {
            try {
                // Clear any previous errors
                error = ""
                
                // Verify the URI is valid and accessible
                val inputStream = context.contentResolver.openInputStream(croppedUri)
                if (inputStream == null) {
                    println("[ProfileSettings] Cannot open input stream for cropped image")
                    error = "Failed to process cropped image"
                    return@rememberLauncherForActivityResult
                }
                
                // Create a copy of the cropped image to ensure we have stable access
                val tempFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
                val outputStream = FileOutputStream(tempFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                // Convert to content:// URI
                val finalUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                
                println("[ProfileSettings] Created final profile image URI: $finalUri")
                profileImageUri = finalUri
                profileImageUrl = null
                
                // Auto-save with cropped image
                profileManager.triggerSave(
                    name = name,
                    username = username,
                    bio = bio,
                    originalUsername = originalUsername,
                    profileImageUrl = profileImageUrl,
                    profileImageUri = profileImageUri,
                    usernameAvailable = usernameAvailable
                )
            } catch (e: Exception) {
                println("[ProfileSettings] Error processing cropped image: ${e.message}")
                e.printStackTrace()
                error = "Error processing image: ${e.message}"
            }
        } else {
            println("[ProfileSettings] Cropped image URI is null")
            error = "Failed to crop image"
        }
    }
    
    // Gallery picker launcher with content:// URIs
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { originalUri ->
            try {
                // Make a copy of the image to our app's cache directory to ensure we have full permissions
                val inputStream = context.contentResolver.openInputStream(originalUri)
                if (inputStream != null) {
                    // Create a temp file to store the image
                    val tempFile = File.createTempFile("image_", ".jpg", context.cacheDir)
                    val outputStream = tempFile.outputStream()
                    
                    // Copy the image data
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    // Create a content:// URI using FileProvider instead of file:// URI
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    
                    // Grant read permissions to the URI for the crop activity
                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.grantUriPermission(context.packageName, contentUri, flag)
                    
                    // If username is the same as original, ensure it's marked as available
                    if (username == originalUsername) {
                        usernameAvailable = true
                    }
                    
                    // Clear any previous errors
                    error = ""
                    
                    // Launch the crop intent with the content URI
                    cropImageLauncher.launch(contentUri)
                } else {
                    error = "Failed to open selected image"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = "Error processing image: ${e.message}"
            }
        }
    }
    
    // Camera launcher with improved content:// URI handling
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // The camera photo is already stored at tempCameraImageUri
            // We can launch the crop activity directly as this URI is already a content:// URI
            tempCameraImageUri?.let { uri ->
                try {
                    // If username is the same as original, ensure it's marked as available
                    if (username == originalUsername) {
                        usernameAvailable = true
                    }
                    
                    // Clear any previous errors
                    error = ""
                    
                    // The camera has already saved the image to our URI, which is a content:// URI
                    // We can launch the cropper directly
                    cropImageLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    error = "Error processing camera image: ${e.message}"
                }
            }
        } else {
            error = "Failed to capture image from camera"
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch camera
            launchCamera(context) { uri ->
                tempCameraImageUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            // Show error message
            error = "Camera permission is required to take photos"
        }
    }

    // Handle back button press
    BackHandler {
        onBackPressed()
    }
    
    // Load user profile data and theme preferences
    LaunchedEffect(Unit) {
        profileManager.loadUserData()
        val themePrefs = profileManager.loadThemePreferences()
        currentThemeIndex = themePrefs.first
        animatedThemeEnabled = themePrefs.second
        loading = false
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings") },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = theme.groupAccent
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    // Add refresh button
                    IconButton(onClick = {
                        // Force reload profile data with cache clearing
                        scope.launch {
                            try {
                                println("[ProfileSettings] Force refreshing profile data and clearing cache")
                                profileManager.forceClearProfileImageCache()
                                android.widget.Toast.makeText(
                                    context,
                                    "Profile refreshed",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                println("[ProfileSettings] Error refreshing profile: ${e.message}")
                                error = "Failed to refresh: ${e.message}"
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = theme.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.card,
                    titleContentColor = theme.textPrimary
                )
            )
        },
        containerColor = theme.background
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.groupAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Profile image with preview on click
                    if (profileImageUri != null) {
                        println("[ProfileUI] Displaying local profile image: $profileImageUri")
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(profileImageUri)
                                .crossfade(true)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_report_image)
                                .build(),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, theme.groupAccent, CircleShape)
                                .clickable { showProfileImagePreview = profileImageUri.toString() },
                            contentScale = ContentScale.Crop,
                            onLoading = { println("[ProfileUI] Loading local profile image: $profileImageUri") },
                            onSuccess = { println("[ProfileUI] Successfully loaded local profile image: $profileImageUri") },
                            onError = { println("[ProfileUI] Error loading local profile image: $profileImageUri - ${it.result.throwable?.message}") }
                        )
                    } else if (profileImageUrl != null) {
                        println("[ProfileUI] Displaying remote profile image: $profileImageUrl")
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(profileImageUrl + "?t=${System.currentTimeMillis()}")
                                .crossfade(true)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_report_image)
                                .build(),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, theme.groupAccent, CircleShape)
                                .clickable { showProfileImagePreview = profileImageUrl },
                            contentScale = ContentScale.Crop,
                            onLoading = { println("[ProfileUI] Loading profile image: $profileImageUrl") },
                            onSuccess = { println("[ProfileUI] Successfully loaded profile image: $profileImageUrl") },
                            onError = { println("[ProfileUI] Error loading profile image: $profileImageUrl - ${it.result.throwable?.message}") }
                        )
                    } else {
                        println("[ProfileUI] Displaying default profile image for name: $name")
                        // Default profile image (first letter of name in a circle)
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(theme.groupAccent.copy(alpha = 0.2f))
                                .border(2.dp, theme.groupAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (name.firstOrNull() ?: "U").toString().uppercase(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.groupAccent
                            )
                        }
                    }

                    // Edit icon over the image
                    IconButton(
                        onClick = { showImagePickerOptions = true },
                        modifier = Modifier
                            .background(theme.groupAccent, CircleShape)
                            .padding(2.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Form fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        // Auto-save when name changes
                        profileManager.triggerSave(
                            name = name,
                            username = username,
                            bio = bio,
                            originalUsername = originalUsername,
                            profileImageUrl = profileImageUrl,
                            profileImageUri = profileImageUri,
                            usernameAvailable = usernameAvailable
                        )
                    },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.groupAccent,
                        unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.5f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it 
                        // Only check availability if username is different from original
                        if (it != originalUsername) {
                            usernameAvailable = null
                            if (it.length >= 3) {
                                profileManager.checkUsernameAvailability(it, originalUsername, name, bio, profileImageUrl, profileImageUri)
                            }
                        } else {
                            usernameAvailable = true
                        }
                    },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                    trailingIcon = {
                        when (usernameAvailable) {
                            true -> Icon(Icons.Default.Check, contentDescription = "Available", tint = Color.Green)
                            false -> Icon(Icons.Default.Close, contentDescription = "Unavailable", tint = Color.Red)
                            null -> if (username.length >= 3 && username != originalUsername) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.groupAccent,
                        unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.5f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { 
                        bio = it
                        // Auto-save when bio changes
                        profileManager.triggerSave(
                            name = name,
                            username = username,
                            bio = bio,
                            originalUsername = originalUsername,
                            profileImageUrl = profileImageUrl,
                            profileImageUri = profileImageUri,
                            usernameAvailable = usernameAvailable
                        )
                    },
                    label = { Text("Bio") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.groupAccent,
                        unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.5f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Status and error messages
                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (success.isNotEmpty()) {
                    Text(
                        text = success,
                        color = Color.Green,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Theme settings section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.card.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Appearance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current theme display with change button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(konvoThemes[currentThemeIndex].groupAccent)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Theme",
                                    fontSize = 16.sp,
                                    color = theme.textPrimary
                                )
                                Text(
                                    konvoThemes[currentThemeIndex].name,
                                    fontSize = 14.sp,
                                    color = theme.textSecondary
                                )
                            }
                            
                            Icon(
                                Icons.Default.ChevronRight, 
                                contentDescription = "Change Theme",
                                tint = theme.textSecondary
                            )
                        }
                        
                        Divider(color = theme.textSecondary.copy(alpha = 0.1f))
                        
                        // Animated background toggle
                        if (konvoThemes[currentThemeIndex].animatedBackground) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Animated Background",
                                    tint = if (animatedThemeEnabled) theme.groupAccent else theme.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    "Animated Background",
                                    fontSize = 16.sp,
                                    color = theme.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Switch(
                                    checked = animatedThemeEnabled,
                                    onCheckedChange = { 
                                        animatedThemeEnabled = it
                                        scope.launch {
                                            profileManager.saveThemePrefs(currentThemeIndex, it)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = theme.groupAccent,
                                        checkedTrackColor = theme.groupAccent.copy(alpha = 0.5f),
                                        uncheckedThumbColor = theme.textSecondary,
                                        uncheckedTrackColor = theme.textSecondary.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Logout button
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout Confirmation") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Theme selection dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        konvoThemes.forEachIndexed { idx, t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (currentThemeIndex != idx) {
                                            currentThemeIndex = idx
                                            scope.launch {
                                                profileManager.saveThemePrefs(idx, animatedThemeEnabled)
                                            }
                                        }
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
                                    fontWeight = if (currentThemeIndex == idx) FontWeight.Bold else FontWeight.Normal
                                )
                                if (currentThemeIndex == idx) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = t.groupAccent
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Close")
                    }
                },
                containerColor = theme.card
            )
        }
        
        // Image picker options dialog
        if (showImagePickerOptions) {
            AlertDialog(
                onDismissRequest = { showImagePickerOptions = false },
                title = { Text("Change Profile Picture") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Launch camera
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            launchCamera(context) { uri ->
                                                tempCameraImageUri = uri
                                                cameraLauncher.launch(uri)
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        launchCamera(context) { uri ->
                                            tempCameraImageUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    }
                                    showImagePickerOptions = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Take Photo")
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Launch gallery
                                    galleryLauncher.launch("image/*")
                                    showImagePickerOptions = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Choose from Gallery")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImagePickerOptions = false }) {
                        Text("Cancel")
                    }
                }
            )
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
}

// Helper function to create a temporary file for camera images
private fun launchCamera(context: Context, onUriCreated: (Uri) -> Unit) {
    try {
        // Create a unique file in the app's cache directory for better compatibility
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        
        // Create the URI using FileProvider which works well with Android's content provider
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        
        // Grant URI permissions
        val packageManager = context.packageManager
        val readPermission = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val writePermission = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val resInfoList = packageManager.queryIntentActivities(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                imageUri,
                readPermission or writePermission
            )
        }
        
        onUriCreated(imageUri)
    } catch (e: Exception) {
        e.printStackTrace()
        // Display the error in a toast
        android.widget.Toast.makeText(
            context,
            "Error launching camera: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
} 