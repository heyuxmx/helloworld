package com.heyu.zhudeapp

import android.app.Application
import com.heyu.zhudeapp.di.UploadManager
import com.heyu.zhudeapp.di.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MyApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        UserManager.init(this)
        UploadManager.init(applicationScope)
    }
}
