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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID

object HeyuModule {

    private const val BASE_URL = "https://marth-nongerminative-hedonistically.ngrok-free.dev"
    private const val TAG = "HeyuModule"

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(lenientJson)
        }
    }

    @Serializable private data class LikeRequest(val post_id: Long)
    @Serializable private data class CommentRequest(val post_id: Long, val content: String, val user_id: String)
    @Serializable private data class CreatePostRequest(val content: String, val user_id: String, val image_urls: List<String>, val video_url: String? = null)
    @Serializable private data class UpdateUserRequest(val username: String? = null, val avatar_url: String? = null)
    @Serializable private data class UploadResponse(val url: String)

    suspend fun likePost(postId: Long) {
        try {
            httpClient.post("$BASE_URL/api/posts/rpc/increment_likes") {
                contentType(ContentType.Application.Json)
                setBody(LikeRequest(postId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post", e)
            throw e
        }
    }

    suspend fun addComment(postId: Long, commentText: String, userId: String): Comment {
        return httpClient.post("$BASE_URL/api/comments") {
            contentType(ContentType.Application.Json)
            setBody(CommentRequest(postId, commentText, userId))
        }.body()
    }

    suspend fun getPosts(): List<Post> {
        return try {
            httpClient.get("$BASE_URL/api/posts").body()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch posts", e)
            throw e
        }
    }

    suspend fun createPost(
        content: String,
        imageUrls: List<String> = emptyList(),
        userId: String,
        videoUrl: String? = null
    ): Post {
        return httpClient.post("$BASE_URL/api/posts") {
            contentType(ContentType.Application.Json)
            setBody(CreatePostRequest(content, userId, imageUrls, videoUrl))
        }.body()
    }

    suspend fun getUsers(): List<UserProfile> =
        httpClient.get("$BASE_URL/api/users").body()

    suspend fun getUserById(userId: String): UserProfile? = try {
        httpClient.get("$BASE_URL/api/users/$userId").body()
    } catch (e: Exception) {
        null
    }

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): UserProfile {
        val fileName = "${UUID.randomUUID()}.jpg"
        val uploadResponse: UploadResponse = httpClient.post("$BASE_URL/api/storage/upload/avatars") {
            setBody(MultiPartFormDataContent(formData {
                append("filename", fileName)
                append("file", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.body()
        return updateAvatarUrl(userId, uploadResponse.url)
    }

    suspend fun updateUsername(userId: String, newUsername: String): UserProfile =
        httpClient.patch("$BASE_URL/api/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRequest(username = newUsername))
        }.body()

    suspend fun updateAvatarUrl(userId: String, newAvatarUrl: String): UserProfile =
        httpClient.patch("$BASE_URL/api/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRequest(avatar_url = newAvatarUrl))
        }.body()

    suspend fun deletePost(post: Post) {
        httpClient.delete("$BASE_URL/api/posts/${post.id}")
    }

    suspend fun deleteComment(commentId: Long) {
        httpClient.delete("$BASE_URL/api/comments/$commentId")
    }

    suspend fun uploadPostImage(imageBytes: ByteArray, fileName: String): String {
        val response: UploadResponse = httpClient.post("$BASE_URL/api/storage/upload/post-images") {
            setBody(MultiPartFormDataContent(formData {
                append("filename", fileName)
                append("file", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.body()
        return response.url
    }

    suspend fun uploadPostVideo(videoBytes: ByteArray, fileName: String): String {
        val response: UploadResponse = httpClient.post("$BASE_URL/api/storage/upload/post-images") {
            setBody(MultiPartFormDataContent(formData {
                append("filename", fileName)
                append("file", videoBytes, Headers.build {
                    append(HttpHeaders.ContentType, "video/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }.body()
        return response.url
    }

    fun compressImage(context: Context, uri: Uri, maxDimension: Int = 1080, quality: Int = 75): ByteArray {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
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
}
