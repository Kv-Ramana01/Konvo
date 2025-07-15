package com.example.konvo.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    // Consistent generation of 1-1 chat IDs
    fun getChatId(user1Id: String, user2Id: String): String {
        return if (user1Id < user2Id) {
            "${user1Id}_${user2Id}"
        } else {
            "${user2Id}_${user1Id}"
        }
    }

    fun isValidFirebaseUid(uid: String): Boolean {
        // Firebase UIDs are typically 28 chars, but can be 20+ and alphanumeric
        return uid.length >= 20 && uid.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    // Send notification for new message via Firestore trigger
    private suspend fun triggerMessageNotification(
        chatId: String,
        senderId: String,
        recipientId: String,
        message: String
    ) {
        val firestore = FirebaseFirestore.getInstance()
        
        // This document will be observed by a Firebase Cloud Function
        // that will send the actual push notification
        val notificationData = hashMapOf(
            "chatId" to chatId,
            "senderId" to senderId,
            "recipientId" to recipientId,
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )
        
        firestore.collection("notifications").add(notificationData).await()
    }
    
    // Modified sendMessage with better error handling
    suspend fun sendMessage(fromId: String, toId: String, message: String) {
        try {
        require(isValidFirebaseUid(fromId)) { "fromId is not a valid Firebase UID: $fromId" }
        require(isValidFirebaseUid(toId)) { "toId is not a valid Firebase UID: $toId" }
            
        val firestore = FirebaseFirestore.getInstance()
        val chatId = getChatId(fromId, toId)
        println("[FirestoreRepository.sendMessage] fromId=$fromId, toId=$toId, chatId=$chatId")
            
        val now = System.currentTimeMillis()
        val messageDoc = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()

        val msg = hashMapOf(
            "senderId" to fromId,
            "text" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "localTimestamp" to now,
            "status" to "sent"
        )
            
            // Send the message first
        messageDoc.set(msg).await()

            // After the message is sent successfully, update the chat references
            try {
        // Fetch user profiles robustly
        val fromProfile = firestore.collection("users").document(fromId).get().await()
        val toProfile = firestore.collection("users").document(toId).get().await()
                val fromName = fromProfile.getString("name")
                val fromUsername = fromProfile.getString("username")
                val fromProfileImage = fromProfile.getString("profileImage")
                val toName = toProfile.getString("name")
                val toUsername = toProfile.getString("username")
                val toProfileImage = toProfile.getString("profileImage")

        // Defensive: Only set 'Unknown' if truly missing
        val safeFromName = if (!fromName.isNullOrBlank()) fromName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $fromId")
            "Unknown"
        }
        val safeFromUsername = if (!fromUsername.isNullOrBlank()) fromUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $fromId")
            "Unknown"
        }
        val safeToName = if (!toName.isNullOrBlank()) toName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $toId")
            "Unknown"
        }
        val safeToUsername = if (!toUsername.isNullOrBlank()) toUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $toId")
            "Unknown"
        }

        // Update user chat references
        val chatRefDataFrom = hashMapOf(
            "lastMessage" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(), // String format for backward compatibility
            "lastMessageTimeNumeric" to now, // Numeric format for reliable sorting
            "otherUserId" to toId,
                    "userName" to safeToName,
                    "userUsername" to safeToUsername
                )
                
        // Add profile image if available
        if (toProfileImage != null) {
            chatRefDataFrom["profileImage"] = toProfileImage
        }
                // Get current unread count to increment it properly
                val recipientChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                val recipientChat = recipientChatRef.get().await()
                
                val chatRefDataTo: HashMap<String, Any>
                
                if (recipientChat.exists()) {
                    val currentUnreadCount = recipientChat.getLong("unreadCount") ?: 0
                    println("[FirestoreRepository] Current unread count for recipient $toId: $currentUnreadCount")
                    
                    chatRefDataTo = hashMapOf(
                        "lastMessage" to message,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "lastMessageTime" to now.toString(),
                        "lastMessageTimeNumeric" to now,
                        "otherUserId" to fromId,
            "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to (currentUnreadCount + 1) // Explicitly increment
        )
                    
                    // Add profile image if available
                    if (fromProfileImage != null) {
                        chatRefDataTo["profileImage"] = fromProfileImage
                    }
                } else {
                    println("[FirestoreRepository] Creating new chat for recipient $toId with unread count 1")
                    chatRefDataTo = hashMapOf(
            "lastMessage" to message,
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(),
            "lastMessageTimeNumeric" to now,
            "otherUserId" to fromId,
                        "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to 1 // Start with 1 unread message
                    )
                    
                    // Add profile image if available
                    if (fromProfileImage != null) {
                        chatRefDataTo["profileImage"] = fromProfileImage
                    }
                }
                
                // Use a batch to ensure atomicity of the chat reference updates
                val batch = firestore.batch()
                
                val fromChatRef = firestore.collection("users").document(fromId)
                    .collection("chats").document(chatId)
                    
                val toChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                    
                batch.set(fromChatRef, chatRefDataFrom)
                batch.set(toChatRef, chatRefDataTo)
                
                batch.commit().await()
                
                // Trigger notification
                try {
                    triggerMessageNotification(chatId, fromId, toId, message)
                } catch (e: Exception) {
                    println("[FirestoreRepository] Failed to trigger notification: ${e.message}")
                    // Don't propagate this error since the message was already sent
                }
                
            } catch (e: Exception) {
                println("[FirestoreRepository] Error updating chat references: ${e.message}")
                // Don't propagate this error since the message was already sent
            }
            
        } catch (e: Exception) {
            println("[FirestoreRepository] Error sending message: ${e.message}")
            throw e // Rethrow to let the caller handle it
        }
    }

    suspend fun sendImageMessage(
        fromId: String,
        toId: String,
        imageUri: android.net.Uri,
        caption: String = "",
        storage: com.google.firebase.storage.FirebaseStorage = com.google.firebase.storage.FirebaseStorage.getInstance(),
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): String? {
        try {
            require(isValidFirebaseUid(fromId)) { "fromId is not a valid Firebase UID: $fromId" }
            require(isValidFirebaseUid(toId)) { "toId is not a valid Firebase UID: $toId" }
            
        val chatId = getChatId(fromId, toId)
        val now = System.currentTimeMillis()
            println("[FirestoreRepository.sendImageMessage] fromId=$fromId, toId=$toId, chatId=$chatId")
            
            // Upload the image first
        val fileRef = storage.reference.child("chat_images/${now}_${imageUri.lastPathSegment}")
        val uploadTask = fileRef.putFile(imageUri).await()
        val downloadUrl = fileRef.downloadUrl.await().toString()
            
            // Create message document
        val messageDoc = firestore.collection("chats").document(chatId).collection("messages").document()
        val msg = hashMapOf(
            "text" to caption,
            "mediaUrl" to downloadUrl,
            "type" to "image",
            "senderId" to fromId,
                "senderName" to "You", // Will be replaced later
            "timestamp" to com.google.firebase.Timestamp.now(),
            "localTimestamp" to now,
            "status" to "sent"
        )
            
            // Save message
        messageDoc.set(msg).await()
            
            // Then update chat references
            try {
        // Fetch user profiles robustly
        val fromProfile = firestore.collection("users").document(fromId).get().await()
        val toProfile = firestore.collection("users").document(toId).get().await()
                val fromName = fromProfile.getString("name")
                val fromUsername = fromProfile.getString("username")
                val fromProfileImage = fromProfile.getString("profileImage")
                val toName = toProfile.getString("name")
                val toUsername = toProfile.getString("username")
                val toProfileImage = toProfile.getString("profileImage")
                
        val safeFromName = if (!fromName.isNullOrBlank()) fromName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $fromId")
            "Unknown"
        }
        val safeFromUsername = if (!fromUsername.isNullOrBlank()) fromUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $fromId")
            "Unknown"
        }
        val safeToName = if (!toName.isNullOrBlank()) toName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $toId")
            "Unknown"
        }
        val safeToUsername = if (!toUsername.isNullOrBlank()) toUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $toId")
            "Unknown"
        }
                
                // Update message with sender name
                messageDoc.update("senderName", safeFromName).await()
                
        // Update chat references for both users
        val chatRefDataFrom = hashMapOf(
            "lastMessage" to (caption.ifEmpty { "[Image]" }),
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(),
            "otherUserId" to toId,
                    "userName" to safeToName,
                    "userUsername" to safeToUsername
                )
                
        // Add profile image if available
        if (toProfileImage != null) {
            chatRefDataFrom["profileImage"] = toProfileImage
        }
                // Get current unread count to increment it properly
                val recipientChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                val recipientChat = recipientChatRef.get().await()
                
                val chatRefDataTo: HashMap<String, Any>
                
                if (recipientChat.exists()) {
                    val currentUnreadCount = recipientChat.getLong("unreadCount") ?: 0
                    println("[FirestoreRepository] Current unread count for recipient $toId: $currentUnreadCount")
                    
                    chatRefDataTo = hashMapOf(
                        "lastMessage" to (caption.ifEmpty { "[Image]" }),
                        "timestamp" to FieldValue.serverTimestamp(),
                        "lastMessageTime" to now.toString(),
                        "lastMessageTimeNumeric" to now,
                        "otherUserId" to fromId,
            "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to (currentUnreadCount + 1) // Explicitly increment
        )
                    
                    // Add profile image if available
                    if (fromProfileImage != null) {
                        chatRefDataTo["profileImage"] = fromProfileImage
                    }
                } else {
                    println("[FirestoreRepository] Creating new chat for recipient $toId with unread count 1")
                    chatRefDataTo = hashMapOf(
            "lastMessage" to (caption.ifEmpty { "[Image]" }),
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(),
            "lastMessageTimeNumeric" to now,
            "otherUserId" to fromId,
                        "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to 1 // Start with 1 unread message
                    )
                    
                    // Add profile image if available
                    if (fromProfileImage != null) {
                        chatRefDataTo["profileImage"] = fromProfileImage
                    }
                }
                
                // Use a batch to ensure atomicity of the chat reference updates
                val batch = firestore.batch()
                
                val fromChatRef = firestore.collection("users").document(fromId)
                    .collection("chats").document(chatId)
                    
                val toChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                    
                batch.set(fromChatRef, chatRefDataFrom)
                batch.set(toChatRef, chatRefDataTo)
                
                batch.commit().await()
                
                // Trigger notification
                try {
                    triggerMessageNotification(chatId, fromId, toId, caption.ifEmpty { "[Image]" })
                } catch (e: Exception) {
                    println("[FirestoreRepository] Failed to trigger notification: ${e.message}")
                    // Don't propagate this error since the message was already sent
                }
                
            } catch (e: Exception) {
                println("[FirestoreRepository] Error updating chat references after image upload: ${e.message}")
                // Don't propagate this error since the message was already sent
            }
            
        return messageDoc.id
            
        } catch (e: Exception) {
            println("[FirestoreRepository] Error sending image message: ${e.message}")
            throw e // Rethrow to let the caller handle it
        }
    }

    suspend fun sendDocumentMessage(
        fromId: String,
        toId: String,
        docUri: android.net.Uri,
        fileName: String?,
        caption: String = "",
        storage: com.google.firebase.storage.FirebaseStorage = com.google.firebase.storage.FirebaseStorage.getInstance(),
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): String? {
        try {
            require(isValidFirebaseUid(fromId)) { "fromId is not a valid Firebase UID: $fromId" }
            require(isValidFirebaseUid(toId)) { "toId is not a valid Firebase UID: $toId" }
            
        val chatId = getChatId(fromId, toId)
        val now = System.currentTimeMillis()
            println("[FirestoreRepository.sendDocumentMessage] fromId=$fromId, toId=$toId, chatId=$chatId")
            
            // Upload the document first
        val fileRef = storage.reference.child("chat_docs/${now}_${docUri.lastPathSegment}")
        val uploadTask = fileRef.putFile(docUri).await()
        val downloadUrl = fileRef.downloadUrl.await().toString()
            
            // Create message document
        val messageDoc = firestore.collection("chats").document(chatId).collection("messages").document()
        val msg = hashMapOf(
            "text" to caption,
            "mediaUrl" to downloadUrl,
            "type" to "document",
            "fileName" to (fileName ?: docUri.lastPathSegment),
            "fileSize" to uploadTask.metadata?.sizeBytes,
            "senderId" to fromId,
                "senderName" to "You", // Will be replaced later
            "timestamp" to com.google.firebase.Timestamp.now(),
            "localTimestamp" to now,
            "status" to "sent"
        )
            
            // Save message
        messageDoc.set(msg).await()
            
            // Then update chat references
            try {
        // Fetch user profiles robustly
        val fromProfile = firestore.collection("users").document(fromId).get().await()
        val toProfile = firestore.collection("users").document(toId).get().await()
                val fromName = fromProfile.getString("name")
                val fromUsername = fromProfile.getString("username")
                val toName = toProfile.getString("name")
                val toUsername = toProfile.getString("username")
                
        val safeFromName = if (!fromName.isNullOrBlank()) fromName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $fromId")
            "Unknown"
        }
        val safeFromUsername = if (!fromUsername.isNullOrBlank()) fromUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $fromId")
            "Unknown"
        }
        val safeToName = if (!toName.isNullOrBlank()) toName else {
                    println("[FirestoreRepository] WARNING: Missing name for user $toId")
            "Unknown"
        }
        val safeToUsername = if (!toUsername.isNullOrBlank()) toUsername else {
                    println("[FirestoreRepository] WARNING: Missing username for user $toId")
            "Unknown"
        }
                
                // Update message with sender name
                messageDoc.update("senderName", safeFromName).await()
                
        // Update chat references for both users
        val chatRefDataFrom = hashMapOf(
            "lastMessage" to (caption.ifEmpty { "[Document]" }),
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(),
            "otherUserId" to toId,
                    "userName" to safeToName,
                    "userUsername" to safeToUsername
                )
                // Get current unread count to increment it properly
                val recipientChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                val recipientChat = recipientChatRef.get().await()
                
                val chatRefDataTo: HashMap<String, Any>
                
                if (recipientChat.exists()) {
                    val currentUnreadCount = recipientChat.getLong("unreadCount") ?: 0
                    println("[FirestoreRepository] Current unread count for recipient $toId: $currentUnreadCount")
                    
                    chatRefDataTo = hashMapOf(
                        "lastMessage" to (caption.ifEmpty { "[Document]" }),
                        "timestamp" to FieldValue.serverTimestamp(),
                        "lastMessageTime" to now.toString(),
                        "lastMessageTimeNumeric" to now,
                        "otherUserId" to fromId,
            "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to (currentUnreadCount + 1) // Explicitly increment
        )
                } else {
                    println("[FirestoreRepository] Creating new chat for recipient $toId with unread count 1")
                    chatRefDataTo = hashMapOf(
            "lastMessage" to (caption.ifEmpty { "[Document]" }),
            "timestamp" to FieldValue.serverTimestamp(),
            "lastMessageTime" to now.toString(),
            "lastMessageTimeNumeric" to now,
            "otherUserId" to fromId,
                        "userName" to safeFromName,
                        "userUsername" to safeFromUsername,
                        "unreadCount" to 1 // Start with 1 unread message
                    )
                }
                
                // Use a batch to ensure atomicity of the chat reference updates
                val batch = firestore.batch()
                
                val fromChatRef = firestore.collection("users").document(fromId)
                    .collection("chats").document(chatId)
                    
                val toChatRef = firestore.collection("users").document(toId)
                    .collection("chats").document(chatId)
                    
                batch.set(fromChatRef, chatRefDataFrom)
                batch.set(toChatRef, chatRefDataTo)
                
                batch.commit().await()
                
                // Trigger notification
                try {
                    triggerMessageNotification(chatId, fromId, toId, caption.ifEmpty { "[Document]" })
                } catch (e: Exception) {
                    println("[FirestoreRepository] Failed to trigger notification: ${e.message}")
                    // Don't propagate this error since the message was already sent
                }
                
            } catch (e: Exception) {
                println("[FirestoreRepository] Error updating chat references after document upload: ${e.message}")
                // Don't propagate this error since the message was already sent
            }
            
        return messageDoc.id
            
        } catch (e: Exception) {
            println("[FirestoreRepository] Error sending document message: ${e.message}")
            throw e // Rethrow to let the caller handle it
        }
    }

    suspend fun createOrUpdateUserChatReference(
        uid: String,
        chatId: String,
        otherUserId: String,
        lastMessage: String,
        timestamp: Any
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()
        val otherProfile = firestore.collection("users").document(otherUserId).get().await()
        val otherName = otherProfile.getString("name")
        val otherUsername = otherProfile.getString("username")
        val safeOtherName = if (!otherName.isNullOrBlank()) otherName else {
            println("[FirestoreRepository] WARNING: Missing name for user $otherUserId")
            "Unknown"
        }
        val safeOtherUsername = if (!otherUsername.isNullOrBlank()) otherUsername else {
            println("[FirestoreRepository] WARNING: Missing username for user $otherUserId")
            "Unknown"
        }
        val chatRefData = hashMapOf(
            "lastMessage" to lastMessage,
            "timestamp" to timestamp,
            "lastMessageTime" to now.toString(),
            "lastMessageTimeNumeric" to now,
            "otherUserId" to otherUserId,
            "userName" to safeOtherName,
            "userUsername" to safeOtherUsername
        )
        firestore.collection("users").document(uid)
            .collection("chats").document(chatId).set(chatRefData).await()
    }

    // Add real-time message listening capabilities
    fun listenForMessages(
        chatId: String, 
        onMessagesUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val firestore = FirebaseFirestore.getInstance()
        
        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            // Add the document ID to the data
                            data["id"] = doc.id
                            data
                        } else null
                    }
                    onMessagesUpdate(messages)
                }
            }
    }
    
    // Listen for chat list updates
    fun listenForChatList(
        userId: String,
        onChatListUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val firestore = FirebaseFirestore.getInstance()
        
        return firestore.collection("users")
            .document(userId)
            .collection("chats")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            // Add the document ID to the data
                            data["id"] = doc.id
                            data
                        } else null
                    }
                    onChatListUpdate(chats)
                }
            }
    }
    
    // Mark messages as read or delivered
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(messageId)
            .update("status", status)
            .await()
    }
    
    // Mark messages as read in batch - fixed to handle non-existent documents
    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        
        try {
            // First check if the chat document exists for this user
            val chatRef = firestore.collection("users")
                .document(userId)
                .collection("chats")
                .document(chatId)
            
            val chatDoc = chatRef.get().await()
            
            // Get all messages in the chat
            val messages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", userId) // Only use one whereNotEqualTo filter
                .get()
                .await()
                
            val batch = firestore.batch()
            val now = System.currentTimeMillis()
            
            // Then filter for unread messages in code
            val unreadMessages = messages.documents.filter { doc ->
                val status = doc.getString("status")
                status != "read"
            }
            
            unreadMessages.forEach { doc ->
                val messageRef = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(doc.id)
                    
                batch.update(messageRef, mapOf(
                    "status" to "read",
                    "readAt" to now
                ))
            }
            
                            // Update or create the chat document with unread count set to 0
                if (chatDoc.exists()) {
                    // Document exists, update it - ensure we reset unread to 0
                    println("[FirestoreRepository] Resetting unread count to 0 for chat $chatId user $userId")
                    batch.update(chatRef, "unreadCount", 0)
                } else {
                // Document doesn't exist, we need to create it
                // First get information about the other user
                val chatIdParts = chatId.split("_")
                val otherUid = if (chatIdParts[0] == userId) chatIdParts[1] else chatIdParts[0]
                
                val otherUserDoc = firestore.collection("users").document(otherUid).get().await()
                val otherUserName = otherUserDoc.getString("name") ?: "Unknown"
                val otherUserUsername = otherUserDoc.getString("username") ?: "Unknown"
                
                val chatData = hashMapOf(
                    "lastMessage" to "",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "lastMessageTime" to System.currentTimeMillis().toString(),
                    "lastMessageTimeNumeric" to System.currentTimeMillis(),
                    "otherUserId" to otherUid,
                    "userName" to otherUserName,
                    "userUsername" to otherUserUsername,
                    "unreadCount" to 0
                )
                
                batch.set(chatRef, chatData)
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            println("[FirestoreRepository] Error marking messages as read: ${e.message}")
            // Don't throw the exception - best effort approach
        }
    }
    
    // Increment unread count for recipient - enhanced with better error handling and creation
    suspend fun incrementUnreadCount(recipientId: String, chatId: String) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            // First check if the chat document exists
            val chatRef = firestore.collection("users")
                .document(recipientId)
                .collection("chats")
                .document(chatId)
                
            val chatDoc = chatRef.get().await()
            
            if (chatDoc.exists()) {
                // Document exists, increment unread count
                val currentUnreadCount = chatDoc.getLong("unreadCount") ?: 0
                println("[FirestoreRepository] Current unread count for $recipientId: $currentUnreadCount")
                
                // Use a transaction to ensure we're correctly updating the count
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(chatRef)
                    val updatedCount = (snapshot.getLong("unreadCount") ?: 0) + 1
                    transaction.update(chatRef, "unreadCount", updatedCount)
                }.await()
            } else {
                // Chat document doesn't exist for recipient
                // We need to create it with basic information
                println("[FirestoreRepository] Creating new chat document for recipient $recipientId")
                
                // Get sender information to use in the chat reference
                val chatIdParts = chatId.split("_")
                val senderId = if (chatIdParts[0] == recipientId) chatIdParts[1] else chatIdParts[0]
                
                val senderDoc = firestore.collection("users").document(senderId).get().await()
                val senderName = senderDoc.getString("name") ?: "Unknown"
                val senderUsername = senderDoc.getString("username") ?: "Unknown"
                
                // Create a new chat reference with unread count of 1
                val chatData = hashMapOf(
                    "lastMessage" to "New message",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "lastMessageTime" to System.currentTimeMillis().toString(),
                    "lastMessageTimeNumeric" to System.currentTimeMillis(),
                    "otherUserId" to senderId,
                    "userName" to senderName,
                    "userUsername" to senderUsername,
                    "unreadCount" to 1
                )
                
                chatRef.set(chatData).await()
            }
        } catch (e: Exception) {
            println("[FirestoreRepository] Error incrementing unread count: ${e.message}")
            // Don't throw the exception as this is a non-critical operation
        }
    }
    
    // Get user profile by username
    suspend fun getUserByUsername(username: String): UserProfile? {
        val firestore = FirebaseFirestore.getInstance()
        val result = firestore.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .await()
            
        if (result.documents.isEmpty()) return null
        
        val doc = result.documents.first()
        return UserProfile(
            uid = doc.id,
            name = doc.getString("name") ?: "Unknown",
            username = doc.getString("username") ?: ""
        )
    }

    // Check if a user is online
    suspend fun getUserOnlineStatus(userId: String): Boolean {
        val firestore = FirebaseFirestore.getInstance()
        val user = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            
        return user.getBoolean("isOnline") ?: false
    }
    
    // Update user online status
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        val firestore = FirebaseFirestore.getInstance()
        val updates = mapOf(
            "isOnline" to isOnline,
            "lastSeen" to if (!isOnline) System.currentTimeMillis() else null
        )
        
        firestore.collection("users")
            .document(userId)
            .update(updates)
            .await()
    }
}

// Define UserProfile data class to match the one in UserSearchScreen.kt
data class UserProfile(
    val uid: String,
    val name: String,
    val username: String
) 