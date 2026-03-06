package com.heyu.zhudeapp.di

import android.content.Context
import android.content.SharedPreferences
import com.heyu.zhudeapp.BuildConfig
import com.heyu.zhudeapp.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn



/**
 * A singleton object responsible for managing the current user, selected upon first launch.
 */
object UserManager {

    private const val XIAOGAO_USER_ID = "12345"
    private const val XIAOXU_USER_ID = "67890"

    private const val XIAOGAO_USER_NAME = "高猪猪"
    private const val XIAOXU_USER_NAME = "徐大王"

    private const val XIAOGAO_PHONE_NUMBER = "19086026159"
    private const val XIAOXU_PHONE_NUMBER = "13098463363"

    private const val PREFS_NAME = "UserPrefs"
    private const val KEY_USER_ID = "selected_user_id"

    private var sharedPreferences: SharedPreferences? = null

    /**
     * Initialize the UserManager with the application context.
     * Must be called once, preferably from the Application class.
     */
    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Sets the current user based on the given user name ("高猪猪" or "徐大王").
     * @param userName The name of the user to set.
     */
    fun setCurrentUser(userName: String) {
        val userId = when (userName) {
            "高猪猪" -> getXiaogaoId()
            "徐大王" -> getXiaoxuId()
            else -> null
        }
        userId?.let {
            setSelectedUser(it)
        }
    }

    /**
     * Saves the chosen user ID to persistent storage.
     * @param userId The user ID to save ("12345" or "67890").
     */
    fun setSelectedUser(userId: String) {
        sharedPreferences?.edit()?.putString(KEY_USER_ID, userId)?.apply()
    }

    /**
     * Retrieves the currently selected user's ID.
     * @return The saved user ID, or null if no user has been selected yet.
     */
    fun getCurrentUserId(): String? {
        return sharedPreferences?.getString(KEY_USER_ID, null)
    }

    /**
     * Clears the current user from persistent storage.
     */
    fun logout() {
        sharedPreferences?.edit()?.remove(KEY_USER_ID)?.apply()
    }

    /**
     * Gets the name of the *current* user.
     * @return The name of the current user, or null if the current user is not recognized.
     */
    fun getCurrentUserName(): String? {
        return when (getCurrentUserId()) {
            XIAOGAO_USER_ID -> XIAOGAO_USER_NAME
            XIAOXU_USER_ID -> XIAOXU_USER_NAME
            else -> null
        }
    }

    /**
     * Gets the phone number of the *other* user.
     * @return The phone number of the other user, or null if the current user is not recognized.
     */
    fun getOtherUserPhoneNumber(): String? {
        return when (getCurrentUserId()) {
            XIAOGAO_USER_ID -> XIAOXU_PHONE_NUMBER
            XIAOXU_USER_ID -> XIAOGAO_PHONE_NUMBER
            else -> null
        }
    }

    /**
     * Gets the name of the *other* user.
     * @return The name of the other user, or null if the current user is not recognized.
     */
    fun getOtherUserName(): String? {
        return when (getCurrentUserId()) {
            XIAOGAO_USER_ID -> XIAOXU_USER_NAME
            XIAOXU_USER_ID -> XIAOGAO_USER_NAME
            else -> null
        }
    }


    /**
     * @return The hardcoded user ID for "高猪猪".
     */
    fun getXiaogaoId(): String = XIAOGAO_USER_ID

    /**
     * @return The hardcoded user ID for "徐大王".
     */
    fun getXiaoxuId(): String = XIAOXU_USER_ID

    /**
     * Fetches the full profile of the currently selected user from the database.
     * @return A Flow that emits the UserProfile, or null if no user is selected or found.
     */
    fun getCurrentUser(): Flow<UserProfile?> = flow {
        val userId = getCurrentUserId()
        if (userId != null) {
            val userProfile = HeyuModule.getUserById(userId)
            if (userProfile != null) {
                emit(userProfile)
            } else {
                // Server unreachable but user ID is saved — emit fallback to avoid dialog loop
                val fallbackName = when (userId) {
                    XIAOGAO_USER_ID -> XIAOGAO_USER_NAME
                    XIAOXU_USER_ID -> XIAOXU_USER_NAME
                    else -> ""
                }
                emit(UserProfile(id = userId, username = fallbackName))
            }
        } else {
            emit(null) // No user selected — trigger identity selection dialog
        }
    }.flowOn(Dispatchers.IO)
}
