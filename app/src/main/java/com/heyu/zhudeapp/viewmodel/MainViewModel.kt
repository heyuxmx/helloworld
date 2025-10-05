package com.heyu.zhudeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _navigateToPost = MutableSharedFlow<String>()
    val navigateToPost = _navigateToPost.asSharedFlow()

    fun onPostIdReceived(postId: String) {
        viewModelScope.launch {
            _navigateToPost.emit(postId)
        }
    }

    /**
     * Call this after the navigation event has been handled
     * to prevent it from being triggered again.
     */
    fun onNavigationComplete() {
        viewModelScope.launch {
            // Emitting a blank string will not trigger the navigation
            // in the fragment because of the `isNotBlank()` check.
            _navigateToPost.emit("")
        }
    }
}
