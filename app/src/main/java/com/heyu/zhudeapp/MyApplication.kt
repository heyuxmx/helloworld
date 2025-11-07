package com.heyu.zhudeapp

import android.app.Application

/**
 * Custom Application class.
 * Initialization of libraries like Firebase is handled automatically by the Google Services plugin.
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Manual Firebase initialization was removed from here.
        // The Google Services Gradle plugin handles initialization automatically via a ContentProvider.
        // A second manual call was causing the app to crash on startup.
    }
}
