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
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
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

    // 定义一个高容错性的 JSON 解析器
    private val lenientJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true // 核心修复：如果后端返回 null 但字段有默认值，自动使用默认值
        encodeDefaults = true
        explicitNulls = false
    }

    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://bvgtzgxscnqhugjirgzp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2Z3R6Z3hzY25xaHVnamlyZ3pwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MDA5NTYsImV4cCI6MjA3NTA3Njk1Nn0.bSF7FkLgvFwsJOODgG8AKtLBpF-OPyzaUfoWSUmoFes"
    ) {
        // 应用全局序列化器配置，确保所有的 Supabase 模块（Postgrest, Auth 等）都使用这个配置
        defaultSerializer = KotlinXSerializer(lenientJson)
        
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    /**
     * 点赞功能
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
     * 添加评论
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
     * 获取动态列表 - 优化加固版：
     * 1. 使用显式关联 (!user_id, !post_id) 确保复杂环境下查询不失败。
     * 2. 适当降低 limit 到 200，防止国外服务器单次请求数据量过大导致超时。
     */
    suspend fun getPosts(): List<Post> {
        return try {
            supabase.postgrest[POST_TABLE].select(
                columns = Columns.raw("""
                    *,
                    author:users!user_id(*),
                    comments:comments!post_id(*, author:users!user_id(*))
                """.trimIndent())
            ) {
                order("created_at", Order.DESCENDING)
                limit(200) 
            }.decodeList<Post>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch posts with nested query", e)
            throw e
        }
    }

    /**
     * 创建动态
     */
    suspend fun createPost(
        content: String, 
        imageUrls: List<String> = emptyList(), 
        userId: String,
        videoUrl: String? = null
    ): Post {
        val newPost = Post(
            content = content,
            userId = userId,
            imageUrls = imageUrls,
            videoUrl = videoUrl
        )

        val result = supabase.postgrest[POST_TABLE].insert<Post>(newPost) {
            select()
        }.decodeList<Post>()

        if (result.isEmpty()) {
            throw IllegalStateException("Post creation failed.")
        }

        return result.first()
    }

    suspend fun getUsers(): List<UserProfile> = supabase.postgrest[PROFILES_TABLE].select { order("created_at", Order.DESCENDING) }.decodeList<UserProfile>()

    suspend fun getUserById(userId: String): UserProfile? = try { supabase.postgrest[PROFILES_TABLE].select { filter { eq("id", userId) } }.decodeList<UserProfile>().firstOrNull() } catch (e: Exception) { null }

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

    suspend fun uploadPostVideo(videoBytes: ByteArray, fileName: String): String {
        supabase.storage.from(POST_IMAGES_BUCKET).upload(path = fileName, data = videoBytes)
        return supabase.storage.from(POST_IMAGES_BUCKET).publicUrl(fileName)
    }
}
