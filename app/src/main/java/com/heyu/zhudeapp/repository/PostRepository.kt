package com.heyu.zhudeapp.repository

import android.content.Context
import android.util.Log
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.database.AppDatabase
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.flow.Flow

/**
 * 数据仓库：实现“离线优先”逻辑的核心
 */
class PostRepository(context: Context) {
    private val postDao = AppDatabase.getDatabase(context).postDao()

    // 暴露数据库的 Flow，Room 会在数据库内容变化时自动通知 UI
    fun getPostsFlow(): Flow<List<Post>> = postDao.getAllPosts()

    /**
     * 刷新数据：从网络获取并静默更新本地数据库
     */
    suspend fun refreshPosts() {
        try {
            // 使用优化后的嵌套查询一次性拿回所有数据
            val remotePosts = SupabaseModule.getPosts()
            if (remotePosts.isNotEmpty()) {
                // 更新本地缓存：存在的会被替换（更新），不存在的会被插入
                postDao.insertPosts(remotePosts)
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "网络同步失败，将继续使用本地缓存: ${e.message}")
            throw e // 抛出异常让 ViewModel 决定是否提示用户
        }
    }

    suspend fun deletePost(post: Post) {
        SupabaseModule.deletePost(post)
        postDao.deleteById(post.id)
    }
}
