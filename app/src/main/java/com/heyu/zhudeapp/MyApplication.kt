package com.heyu.zhudeapp

import android.app.Application
import com.heyu.zhudeapp.di.UserManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.init(this)
    }
}
