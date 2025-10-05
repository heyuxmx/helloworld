package com.heyu.zhudeapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Custom Application class to ensure Firebase is initialized manually.
 * This class runs before any Activity and is the ideal place for app-wide initialization.
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "Application onCreate: Manual Firebase initialization starting.")
        try {
            // Force Firebase to initialize using the context of this Application class.
            FirebaseApp.initializeApp(this)
            Log.d("MyApplication", "FirebaseApp.initializeApp(this) completed successfully.")
        } catch (e: Exception) {
            // If this fails, it will now produce a loud, explicit error in Logcat.
            Log.e("MyApplication", "CRITICAL: Firebase initialization failed", e)
        }
    }
}
