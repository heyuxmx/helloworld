package com.heyu.zhudeapp.di

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.data.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 全局Supabase模块，提供一个单例的SupabaseClient实例，并封装了常用的功能函数。
 */
object SupabaseModule {

    private const val POST_TABLE = "posts"
    private const val COMMENTS_TABLE = "comments"
    private const val PROFILES_TABLE = "users"
    private const val POST_IMAGES_BUCKET = "post-images"
    private const val AVATARS_BUCKET = "avatars"
    private const val TAG = "SupabaseModule"

    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://bvgtzgxscnqhugjirgzp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2Z3R6Z3hzY25xaHVnamlyZ3pwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MDA5NTYsImV4cCI6MjA3NTA3Njk1Nn0.bSF7FkLgvFwsJOODgG8AKtLBpF-OPyzaUfoWSUmoFes"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    /**
     * 点赞功能的原有代码（保留）
     */
    suspend fun likePost(postId: Long) {
        Log.d(TAG, "Attempting to like post with ID: $postId")
        try {
            supabase.postgrest.rpc(
                function = "increment_likes",
                parameters = buildJsonObject {
                    put("post_id", postId) 
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calling increment_likes RPC", e)
            throw e
        }
    }


    /**
     * 添加评论的原有代码（保留）
     */
    suspend fun addComment(postId: Long, commentText: String, userId: String): Comment {
        val newComment = Comment(
            postId = postId,
            content = commentText,
            userId = userId
        )

        val result = supabase.postgrest[COMMENTS_TABLE].insert(newComment) {
            select()
        }.decodeList<Comment>()

        if (result.isEmpty()) {
            throw IllegalStateException("Comment creation failed.")
        }
        return result.first()
    }


    /**
     * 获取动态列表的原有代码（保留）
     */
    suspend fun getPosts(): List<Post> {
        val postsWithoutAuthors = supabase.postgrest[POST_TABLE].select {
            order("created_at", Order.DESCENDING)
        }.decodeList<Post>()

        if (postsWithoutAuthors.isEmpty()) return emptyList()

        val postAuthorIds = postsWithoutAuthors.map { it.userId }.distinct()
        val postAuthors = supabase.postgrest[PROFILES_TABLE].select {
            filter { isIn("id", postAuthorIds) }
        }.decodeList<UserProfile>()
        val postAuthorMap = postAuthors.associateBy { it.id }

        val postIds = postsWithoutAuthors.map { it.id }
        val allComments = supabase.postgrest[COMMENTS_TABLE].select {
            filter { isIn("post_id", postIds) }
        }.decodeList<Comment>()

        if (allComments.isNotEmpty()) {
            val commentAuthorIds = allComments.map { it.userId }.distinct()
            val commentAuthors = supabase.postgrest[PROFILES_TABLE].select {
                filter { isIn("id", commentAuthorIds) }
            }.decodeList<UserProfile>()
            val commentAuthorMap = commentAuthors.associateBy { it.id }

            val commentsWithAuthors = allComments.map { it.copy(author = commentAuthorMap[it.userId]) }
            val commentsGroupedByPost = commentsWithAuthors.groupBy { it.postId }

            return postsWithoutAuthors.map { post ->
                post.copy(
                    author = postAuthorMap[post.userId],
                    comments = commentsGroupedByPost[post.id]?.toMutableList() ?: mutableListOf()
                )
            }
        } else {
            return postsWithoutAuthors.map { post ->
                post.copy(
                    author = postAuthorMap[post.userId],
                    comments = mutableListOf()
                )
            }
        }
    }

    /**
     * 创建动态函数（已更新以支持视频链接，同时保留原有功能）
     * @param content 动态文本
     * @param imageUrls 图片列表
     * @param userId 用户ID
     * @param videoUrl 阿里云视频链接（新增）
     */
    suspend fun createPost(
        content: String, 
        imageUrls: List<String> = emptyList(), 
        userId: String,
        videoUrl: String? = null // 新增的可选参数
    ): Post {
        val newPost = Post(
            content = content,
            imageUrls = imageUrls,
            userId = userId,
            videoUrl = videoUrl // 将视频链接存入数据库
        )

        val result = supabase.postgrest[POST_TABLE].insert<Post>(newPost) {
            select()
        }.decodeList<Post>()

        if (result.isEmpty()) {
            throw IllegalStateException("Post creation failed. Please check RLS policies.")
        }

        return result.first()
    }

    // --- 以下是原有的用户资料、头像更新、图片压缩等工具函数，均已原封不动保留 ---

    suspend fun getUsers(): List<UserProfile> = supabase.postgrest[PROFILES_TABLE].select { order("created_at", Order.DESCENDING) }.decodeList<UserProfile>()

    suspend fun getUserById(userId: String): UserProfile? = try { supabase.postgrest[PROFILES_TABLE].select { filter { eq("id", userId) } }.decodeList<UserProfile>().firstOrNull() } catch (e: Exception) { null }

    suspend fun getUserProfile(userId: String): UserProfile? = try { supabase.postgrest[PROFILES_TABLE].select { filter { eq("id", userId) } }.decodeList<UserProfile>().firstOrNull() } catch (e: Exception) { null }

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): UserProfile {
        val fileName = "${UUID.randomUUID()}.jpg"
        supabase.storage.from(AVATARS_BUCKET).upload(path = fileName, data = imageBytes)
        val newAvatarUrl = supabase.storage.from(AVATARS_BUCKET).publicUrl(fileName)
        return updateAvatarUrl(userId, newAvatarUrl)
    }

    suspend fun updateUsername(userId: String, newUsername: String): UserProfile {
        val result = supabase.postgrest[PROFILES_TABLE].update({ set("username", newUsername) }) { filter { eq("id", userId) }; select() }.decodeList<UserProfile>()
        return result.first()
    }

    suspend fun updateAvatarUrl(userId: String, newAvatarUrl: String): UserProfile {
        val result = supabase.postgrest[PROFILES_TABLE].update({ set("avatar_url", newAvatarUrl) }) { filter { eq("id", userId) }; select() }.decodeList<UserProfile>()
        return result.first()
    }

    suspend fun updateUserFcmToken(userId: String, token: String) {
        supabase.postgrest[PROFILES_TABLE].update({ set("fcm_token", token) }) { filter { eq("id", userId) } }
    }

    suspend fun deletePost(post: Post) {
        supabase.postgrest[POST_TABLE].delete { filter { eq("id", post.id) } }
        val result = supabase.postgrest[POST_TABLE].select { filter { eq("id", post.id) } }.decodeList<Post>()
        if (result.isNotEmpty()) throw IllegalStateException("Deletion failed.")
    }

    suspend fun deleteComment(commentId: Long) {
        supabase.postgrest[COMMENTS_TABLE].delete { filter { eq("id", commentId) } }
    }

    suspend fun uploadPostImage(imageBytes: ByteArray, fileName: String): String {
        supabase.storage.from(POST_IMAGES_BUCKET).upload(path = fileName, data = imageBytes)
        return supabase.storage.from(POST_IMAGES_BUCKET).publicUrl(fileName)
    }

    fun compressImage(context: Context, uri: Uri, maxDimension: Int = 1080, quality: Int = 75): ByteArray {
        val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        var srcWidth = options.outWidth.toFloat()
        var srcHeight = options.outHeight.toFloat()
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val scaledBitmap = context.contentResolver.openInputStream(uri)?.use {
            val sourceBitmap = BitmapFactory.decodeStream(it)
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * 原有的 Supabase 视频上传函数（保留，用于上传 50MB 以下的小视频）
     */
    suspend fun uploadPostVideo(videoBytes: ByteArray, fileName: String): String {
        supabase.storage.from(POST_IMAGES_BUCKET).upload(path = fileName, data = videoBytes)
        return supabase.storage.from(POST_IMAGES_BUCKET).publicUrl(fileName)
    }
}
