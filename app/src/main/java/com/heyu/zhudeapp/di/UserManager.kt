package com.heyu.zhudeapp.di

import com.heyu.zhudeapp.BuildConfig
import com.heyu.zhudeapp.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * A singleton object responsible for managing the current user based on the application's build flavor.
 */
object UserManager {

    private const val XIAOGAO_USER_ID = "12345"
    private const val XIAOXU_USER_ID = "67890"

    /**
     * Determines the current user's ID based on the application's build flavor.
     * This is the standard and most reliable way to differentiate between build variants.
     *
     * @return The user ID ('12345' for xiaogao, '67890' for xiaoxu).
     */
    fun getCurrentUserId(): String {
        return when (BuildConfig.FLAVOR) {
            "xiaogao" -> XIAOGAO_USER_ID
            else -> XIAOXU_USER_ID // Default to xiaoxu for any other flavor, including "xiaoxu"
        }
    }

    /**
     * Fetches the full profile of the current user from the database.
     * The user is determined by the app's build flavor.
     *
     * @return A Flow that emits the UserProfile of the current user, or null if not found.
     */
    fun getCurrentUser(): Flow<UserProfile?> = flow {
        val userId = getCurrentUserId()
        val userProfile = SupabaseModule.getUserById(userId)
        emit(userProfile)
    }.flowOn(Dispatchers.IO) // Perform network operations on the IO thread.
}
