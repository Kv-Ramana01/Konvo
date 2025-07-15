package com.example.konvo.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.konvo.KonvoApp
import com.example.konvo.MainActivity
import com.example.konvo.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KonvoMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        // Update the token in Firestore for the current user
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update FCM token in Firestore", e)
                }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            showNotification(notification.title, notification.body)
        }
        
        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val senderId = remoteMessage.data["senderId"]
            val chatId = remoteMessage.data["chatId"]
            val message = remoteMessage.data["message"]
            val senderName = remoteMessage.data["senderName"]
            
            if (senderId != null && chatId != null && message != null) {
                // Don't show notification if the sender is the current user
                if (senderId == Firebase.auth.currentUser?.uid) {
                    return
                }
                
                // Don't show notification if the user is currently in the chat
                // This would require tracking the current active chat in a shared preference or similar
                
                // Show notification
                showNotification(senderName ?: "New message", message)
            }
        }
    }
    
    private fun showNotification(title: String?, body: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, KonvoApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title ?: "New Message")
            .setContentText(body ?: "You have a new message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(this)) {
            try {
                notify(NOTIFICATION_ID, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "Notification permission not granted", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "KonvoMessaging"
        private const val NOTIFICATION_ID = 100
    }
} 