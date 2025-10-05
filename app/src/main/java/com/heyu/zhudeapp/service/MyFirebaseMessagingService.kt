package com.heyu.zhudeapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.MainActivity
import com.heyu.zhudeapp.client.SupabaseClient.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    companion object {
        // Define a constant for the key, ensuring consistency across the app.
        const val EXTRA_POST_ID = "post_id"
    }

    /**
     * Called when a message is received.
     * This method is now structured to handle different types of notifications.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Determine the intent for the notification based on the message content.
        val intent: Intent

        // Case 1: The message is a standard notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Handling notification payload.")
            // This is a simple notification, so it will just open the MainActivity.
            intent = Intent(this, MainActivity::class.java)
            remoteMessage.notification?.let {
                sendNotification(it.title, it.body, intent)
            }
        }
        // Case 2: The message is a data payload, which is more flexible.
        else if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Handling data payload: ${remoteMessage.data}")
            val data = remoteMessage.data
            val title = data["title"]
            val body = data["body"]
            val postId = data[EXTRA_POST_ID] // Check for our custom data.

            // Create a base intent to open MainActivity.
            intent = Intent(this, MainActivity::class.java).apply {
                // If a postId is present, add it as an extra.
                // This allows MainActivity to decide whether to navigate further.
                if (postId != null) {
                    putExtra(EXTRA_POST_ID, postId)
                }
            }

            sendNotification(title, body, intent)
        }
    }

    /**
     * Called when a new token for the default Firebase project is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    /**
     * Persists the FCM registration token to the backend (Supabase).
     */
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.e(TAG, "Cannot send null token to server.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.from("fcm_tokens").upsert(
                    value = mapOf("token" to token),
                    onConflict = "token"
                )
                Log.d(TAG, "Successfully saved refreshed FCM Token to Supabase.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving refreshed FCM Token to Supabase", e)
            }
        }
    }

    /**
     * Creates and displays a notification. This function is now more generic.
     * @param intent The intent to be launched when the notification is clicked.
     */
    private fun sendNotification(messageTitle: String?, messageBody: String?, intent: Intent) {
        // Ensure the intent is only used once and is cleared from the top of the task stack.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        // Use the app name as a fallback if the title is missing.
        val title = messageTitle ?: getString(R.string.app_name)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.house)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo, a notification channel is required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
