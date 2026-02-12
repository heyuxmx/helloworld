package com.heyu.zhudeapp.viewmodel

import android.app.Application
import android.util.Log
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

    // 观察本地数据库的数据流，只要本地有，UI 就能立刻显示
    val posts: LiveData<List<Post>> = repository.getPostsFlow().asLiveData()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _postCreationSuccess = MutableLiveData<Boolean>()
    val postCreationSuccess: LiveData<Boolean> = _postCreationSuccess

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

    fun doneNotifyingSms() {
        _postCreationSuccess.postValue(false)
    }

    /**
     * 核心逻辑：发起网络同步
     */
    fun fetchPosts() {
        viewModelScope.launch {
            try {
                // 仅执行数据库同步，UI 会通过观察 `posts` 自动刷新
                repository.refreshPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Fetch posts failed", e)
                // 显示更具体的错误信息，方便调试
                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "服务器响应超时，请重试"
                    e.message?.contains("JSON", ignoreCase = true) == true -> "数据解析失败，请联系管理员"
                    else -> "连接服务器失败，当前显示为缓存内容"
                }
                _error.postValue(errorMsg)
            }
        }
    }

    suspend fun deletePost(post: Post) {
        try {
            repository.deletePost(post)
        } catch (e: Exception) {
            _error.postValue("删除失败: ${e.message}")
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
                _error.postValue("发布失败: ${e.message}")
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
                _error.postValue("发布失败: ${e.message}")
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
                _error.postValue("评论失败: ${e.message}")
            }
        }
    }
}
