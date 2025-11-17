package com.heyu.zhudeapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PostViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

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

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                // Use postValue as Supabase calls might be on a background thread
                _posts.postValue(SupabaseModule.getPosts())
            } catch (e: Exception) {
                _error.postValue("加载动态失败: ${e.message}")
            }
        }
    }

    suspend fun deletePost(post: Post) {
        // This will propagate the exception up to the calling coroutine scope in the Fragment
        SupabaseModule.deletePost(post)
        // After successful deletion, refresh the posts list.
        fetchPosts()
    }

    suspend fun deleteComment(comment: Comment) {
        SupabaseModule.deleteComment(comment.id)
        fetchPosts()
    }

    fun createTextPost(content: String, userId: String) {
        viewModelScope.launch {
            try {
                SupabaseModule.createPost(content, emptyList(), userId)
                fetchPosts()
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
                fetchPosts()
            } catch (e: Exception) {
                _error.postValue("创建动态失败: ${e.message}")
            }
        }
    }

    fun addComment(postId: Long, commentText: String, userId: String) {
        viewModelScope.launch {
            try {
                SupabaseModule.addComment(postId, commentText, userId)
                clearCommentDraft(postId) // Clear the draft after successful post
                fetchPosts() // Refresh posts to show the new comment
            } catch (e: Exception) {
                _error.postValue("添加评论失败: ${e.message}")
            }
        }
    }
}
