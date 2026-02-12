package com.heyu.zhudeapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heyu.zhudeapp.data.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: Long)
}
