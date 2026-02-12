package com.heyu.zhudeapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    // Using Flow from Room, converted to LiveData
    val posts: LiveData<List<Post>> = repository.getPostsFlow().asLiveData()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _postCreationSuccess = MutableLiveData<Boolean>()
    val postCreationSuccess: LiveData<Boolean> = _postCreationSuccess

    // --- Comment Draft Management ---
    private val _commentDrafts = MutableStateFlow<Map<Long, String>>(emptyMap())
    val commentDrafts = _commentDrafts.asStateFlow()

    fun updateCommentDraft(postId: Long, draft: String) {
        val newDrafts = _commentDrafts.value.toMutableMap()
        newDrafts[postId] = draft
        _commentDrafts.value = newDrafts
    }

    private fun clearCommentDraft(postId: Long) {
        val newDrafts = _commentDrafts.value.toMutableMap()
        newDrafts.remove(postId)
        _commentDrafts.value = newDrafts
    }
    // --------------------------------

    fun doneNotifyingSms() {
        _postCreationSuccess.postValue(false)
    }

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                repository.refreshPosts()
            } catch (e: Exception) {
                _error.postValue("加载最新动态失败: ${e.message}")
            }
        }
    }

    suspend fun deletePost(post: Post) {
        try {
            repository.deletePost(post)
        } catch (e: Exception) {
            _error.postValue("删除动态失败: ${e.message}")
        }
    }

    suspend fun deleteComment(comment: Comment) {
        try {
            SupabaseModule.deleteComment(comment.id)
            fetchPosts()
        } catch (e: Exception) {
            _error.postValue("删除评论失败: ${e.message}")
        }
    }

    fun createTextPost(content: String, userId: String) {
        viewModelScope.launch {
            try {
                SupabaseModule.createPost(content, emptyList(), userId)
                repository.refreshPosts()
                _postCreationSuccess.postValue(true)
            } catch (e: Exception) {
                _error.postValue("创建动态失败: ${e.message}")
            }
        }
    }

    fun createPostWithImage(content: String, imageBytes: ByteArray, fileExtension: String, userId: String) {
        viewModelScope.launch {
            try {
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val imageUrl = SupabaseModule.uploadPostImage(imageBytes, fileName)
                SupabaseModule.createPost(content, listOf(imageUrl), userId)
                repository.refreshPosts()
                _postCreationSuccess.postValue(true)
            } catch (e: Exception) {
                _error.postValue("创建动态失败: ${e.message}")
            }
        }
    }

    fun addComment(postId: Long, commentText: String, userId: String) {
        viewModelScope.launch {
            try {
                SupabaseModule.addComment(postId, commentText, userId)
                clearCommentDraft(postId)
                repository.refreshPosts()
            } catch (e: Exception) {
                _error.postValue("添加评论失败: ${e.message}")
            }
        }
    }
}
