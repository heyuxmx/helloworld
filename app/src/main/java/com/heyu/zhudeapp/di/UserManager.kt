//package com.heyu.zhudeapp.di
//
//import com.heyu.zhudeapp.BuildConfig
//
///**
// * Manages user-specific information based on the current build flavor.
// */
//object UserManager {
//
//    /**
//     * The username for the current build flavor (e.g., "小高猪猪" or "小徐爸爸").
//     * This value is injected at build time from the build.gradle.kts file.
//     */
//    val username: String = BuildConfig.USERNAME
//
//    /**
//     * A simple identifier for the user's avatar (e.g., "xiaogao" or "xiaoxu").
//     * This can be used to decide which drawable or remote URL to load for the avatar.
//     * This value is injected at build time from the build.gradle.kts file.
//     */
//    val avatarIdentifier: String = BuildConfig.AVATAR_IDENTIFIER
//
//}
