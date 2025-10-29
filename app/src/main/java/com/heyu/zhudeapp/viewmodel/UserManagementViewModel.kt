package com.heyu.zhudeapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.UserProfile
import com.heyu.zhudeapp.di.UserManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the User Management screen.
 * This class is responsible for fetching the SINGLE current user based on the app's build flavor
 * and providing it to the UI layer.
 */
class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    // Private MutableLiveData to hold the current user's profile.
    private val _currentUser = MutableLiveData<UserProfile?>()
    // Public LiveData exposed to the UI, which is read-only.
    val currentUser: LiveData<UserProfile?> = _currentUser

    // Private MutableLiveData to hold any error messages.
    private val _error = MutableLiveData<String>()
    // Public LiveData for error messages exposed to the UI.
    val error: LiveData<String> = _error

    /**
     * Fetches the profile of the current user based on the app's build flavor.
     * It launches a coroutine to call the UserManager and collects the resulting Flow.
     */
    fun fetchCurrentUser() {
        viewModelScope.launch {
            UserManager.getCurrentUser()
                .catch { e ->
                    // Handle any exceptions during the flow execution
                    _error.postValue("Failed to fetch current user: ${e.message}")
                }
                .collect { user ->
                    if (user != null) {
                        _currentUser.postValue(user)
                    } else {
                        // This case handles when UserManager returns null (e.g., unknown package name)
                        _error.postValue("Current user could not be determined for this app version.")
                    }
                }
        }
    }
}
