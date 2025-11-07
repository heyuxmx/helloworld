package com.heyu.zhudeapp.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.UserProfile
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.di.UserManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentUser = MutableLiveData<UserProfile?>()
    val currentUser: LiveData<UserProfile?> = _currentUser

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _uploadSuccess = MutableLiveData<Boolean>()
    val uploadSuccess: LiveData<Boolean> = _uploadSuccess

    fun fetchCurrentUser() {
        viewModelScope.launch {
            UserManager.getCurrentUser()
                .catch { e ->
                    _error.postValue("Failed to fetch current user: ${e.message}")
                }
                .collect { user ->
                    if (user != null) {
                        _currentUser.postValue(user)
                    } else {
                        _error.postValue("Current user could not be determined for this app version.")
                    }
                }
        }
    }

    fun uploadAndupdateAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id ?: throw IllegalStateException("User ID is not available.")
                val imageBytes = SupabaseModule.compressImage(getApplication(), uri)
                SupabaseModule.uploadAvatar(userId, imageBytes)
                fetchCurrentUser()
                _uploadSuccess.postValue(true)
            } catch (e: Exception) {
                _error.postValue("Avatar upload failed: ${e.message}")
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id ?: throw IllegalStateException("User ID is not available.")
                SupabaseModule.updateUsername(userId, newUsername)
                fetchCurrentUser()
            } catch (e: Exception) {
                _error.postValue("Failed to update username: ${e.message}")
            }
        }
    }

}
