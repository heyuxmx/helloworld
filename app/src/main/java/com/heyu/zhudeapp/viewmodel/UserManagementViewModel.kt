package com.heyu.zhudeapp.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.UserProfile
import com.heyu.zhudeapp.di.HeyuModule
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
                    _currentUser.postValue(null)
                }
                .collect { user ->
                    _currentUser.postValue(user)
                }
        }
    }

    fun uploadAndupdateAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id ?: throw IllegalStateException("User ID is not available.")
                val imageBytes = HeyuModule.compressImage(getApplication(), uri)
                HeyuModule.uploadAvatar(userId, imageBytes)
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
                HeyuModule.updateUsername(userId, newUsername)
                fetchCurrentUser()
            } catch (e: Exception) {
                _error.postValue("Failed to update username: ${e.message}")
            }
        }
    }

    fun switchUser(userName: String) {
        viewModelScope.launch {
            UserManager.setCurrentUser(userName)
            fetchCurrentUser()
        }
    }

    fun logout() {
        UserManager.logout()
        _currentUser.value = null
    }

}
