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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_DEBUG" // Use a specific tag for easy filtering in Logcat

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // This is the entry point for any push notification received.
        super.onMessageReceived(remoteMessage)

        // --- Start of Diagnostic Logging ---
        Log.d(TAG, ">>> FCM Message Received!")
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Log the data payload (this is what we send from our Edge Function)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        } else {
            Log.d(TAG, "Message data payload is EMPTY.")
        }

        // Log the notification payload (if any, typically not used by our backend)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
        // --- End of Diagnostic Logging ---


        // The original logic to process and show the notification.
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            val postId = remoteMessage.data["postId"]

            if (title != null && body != null) {
                sendNotification(title, body, postId)
            } else {
                Log.d(TAG, "Notification was not shown because title or body was null in data payload.")
            }
        }
    }

    override fun onNewToken(token: String) {
        // This is called when a new token is generated for the device.
        super.onNewToken(token)
        Log.d(TAG, ">>> New FCM Token: $token")
        // The logic to save the token is handled in MainActivity to ensure it's tied to a logged-in user.
    }

    private fun sendNotification(title: String, body: String, postId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_post_id", postId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d(TAG, "Notification sent to system UI.")
    }
}
