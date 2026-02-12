package com.heyu.zhudeapp.repository

import android.content.Context
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.database.AppDatabase
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class PostRepository(context: Context) {
    private val postDao = AppDatabase.getDatabase(context).postDao()

    fun getPostsFlow(): Flow<List<Post>> = postDao.getAllPosts()

    suspend fun refreshPosts() {
        val remotePosts = SupabaseModule.getPosts()
        if (remotePosts.isNotEmpty()) {
            postDao.insertPosts(remotePosts)
        }
    }

    suspend fun deletePost(post: Post) {
        SupabaseModule.deletePost(post)
        postDao.deleteById(post.id)
    }
}
