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
     * Calls an RPC function to increment the likes of a post.
     * This version includes detailed logging for diagnostics and uses a standard parameter name.
     * @param postId The id of the post to like.
     */
    suspend fun likePost(postId: Long) {
        Log.d(TAG, "Attempting to like post with ID: $postId")
        try {
            supabase.postgrest.rpc(
                function = "increment_likes",
                parameters = buildJsonObject {
                    put("post_id", postId) // Using a more standard parameter name.
                }
            )
            Log.d(TAG, "Successfully called increment_likes RPC for post ID: $postId")
        } catch (e: Exception) {
            Log.e(TAG, "Error calling increment_likes RPC for post ID: $postId", e)
            // Re-throw the exception to let the caller know something went wrong.
            throw e
        }
    }


    /**
     * 为指定的帖子添加一条新的评论。
     * @param postId 评论所属的帖子的ID。
     * @param commentText 评论的文本内容。
     * @param userId 评论作者的用户ID。
     * @return 创建成功并从数据库返回的Comment对象。
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
            throw IllegalStateException("Comment creation failed for post ID: $postId. This is likely due to RLS policies.")
        }
        return result.first()
    }


    /**
     * 从数据库获取所有动态的列表，并严格按照创建时间降序排列。
     * This version manually fetches posts, authors, and comments to bypass potential issues
     * with Supabase's automatic relational queries.
     * @return 从新到旧排序的动态列表，包含作者和评论信息。
     */
    suspend fun getPosts(): List<Post> {
        // Step 1: Fetch all posts without any joins.
        val postsWithoutAuthors = supabase.postgrest[POST_TABLE].select {
            order("created_at", Order.DESCENDING)
        }.decodeList<Post>()

        if (postsWithoutAuthors.isEmpty()) {
            return emptyList()
        }

        // Step 2: Collect all unique author IDs from the posts.
        val postAuthorIds = postsWithoutAuthors.map { it.userId }.distinct()

        // Step 3: Fetch all the required authors (users) for the posts.
        val postAuthors = supabase.postgrest[PROFILES_TABLE].select {
            filter {
                isIn("id", postAuthorIds)
            }
        }.decodeList<UserProfile>()
        val postAuthorMap = postAuthors.associateBy { it.id }

        // Step 4: Fetch all comments for the retrieved posts.
        val postIds = postsWithoutAuthors.map { it.id }
        val allComments = supabase.postgrest[COMMENTS_TABLE].select {
            filter {
                isIn("post_id", postIds)
            }
        }.decodeList<Comment>()

        // Step 5: Fetch all authors for the comments if comments exist.
        if (allComments.isNotEmpty()) {
            val commentAuthorIds = allComments.map { it.userId }.distinct()
            val commentAuthors = supabase.postgrest[PROFILES_TABLE].select {
                filter {
                    isIn("id", commentAuthorIds)
                }
            }.decodeList<UserProfile>()
            val commentAuthorMap = commentAuthors.associateBy { it.id }

            // Step 6: Manually 'join' comments with their authors.
            val commentsWithAuthors = allComments.map { it.copy(author = commentAuthorMap[it.userId]) }
            val commentsGroupedByPost = commentsWithAuthors.groupBy { it.postId }

            // Step 7: Manually 'join' posts with their authors and grouped comments.
            return postsWithoutAuthors.map { post ->
                post.copy(
                    author = postAuthorMap[post.userId],
                    comments = commentsGroupedByPost[post.id]?.toMutableList() ?: mutableListOf()
                )
            }
        } else {
            // No comments found, just join posts with their authors.
            return postsWithoutAuthors.map { post ->
                post.copy(
                    author = postAuthorMap[post.userId],
                    comments = mutableListOf() // Ensure comments list is not null
                )
            }
        }
    }

    /**
     * 创建一条新的动态，并验证插入是否成功。
     * 如果插入失败（通常因为RLS策略），会抛出异常。
     * @param content 动态的文本内容。
     * @param imageUrls 可选的图片URL列表。
     * @param userId 动态作者的用户ID。
     * @return 创建成功并从数据库返回的Post对象。
     */
    suspend fun createPost(content: String, imageUrls: List<String> = emptyList(), userId: String): Post {
        val newPost = Post(
            content = content,
            imageUrls = imageUrls,
            userId = userId
        )

        // 步骤 1: 插入数据并请求返回插入的记录
        val result = supabase.postgrest[POST_TABLE].insert<Post>(newPost) {
            select() // 关键：请求将插入的数据返回
        }.decodeList<Post>()

        // 步骤 2: 验证返回的列表是否为空
        if (result.isEmpty()) {
            // 步骤 3: 如果为空，说明插入未成功，抛出描述性异常
            throw IllegalStateException("Post creation failed: The post was not created. This is likely due to Row-Level Security (RLS) policies. Please check the 'INSERT' policy on the 'posts' table in your Supabase dashboard.")
        }

        // 步骤 4: 返回创建成功的Post对象
        return result.first()
    }
    
    /**
     * 获取所有用户的列表，并按照创建时间降序排列。
     * @return 从新到旧排序的用户列表。
     */
    suspend fun getUsers(): List<UserProfile> {
        return supabase.postgrest[PROFILES_TABLE].select {
            order("created_at", Order.DESCENDING)
        }.decodeList<UserProfile>()
    }

    /**
     * 根据用户ID获取单个用户的详细信息。
     * @param userId 要获取的用户的ID。
     * @return 如果找到，则返回UserProfile对象；否则返回null。
     */
    suspend fun getUserById(userId: String): UserProfile? {
        return try {
            supabase.postgrest[PROFILES_TABLE].select {
                filter {
                    eq("id", userId)
                }
            }.decodeList<UserProfile>().firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by ID: $userId", e)
            null
        }
    }

    /**
     * 根据用户ID获取单个用户的详细信息。 (This is the function PostAdapter is calling)
     * @param userId 要获取的用户的ID。
     * @return 如果找到，则返回UserProfile对象；否则返回null。
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            supabase.postgrest[PROFILES_TABLE].select {
                filter {
                    eq("id", userId)
                }
            }.decodeList<UserProfile>().firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile by ID: $userId", e)
            null
        }
    }

    /**
     * 上传新的用户头像，并更新用户的个人资料。
     *
     * @param userId 用户的ID。
     * @param imageBytes 要上传的头像图片的字节数组。
     * @return 更新成功后的UserProfile对象。
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): UserProfile {
        // 1. 为头像生成一个独一无二的文件名，防止冲突。
        val fileName = "${UUID.randomUUID()}.jpg"

        // 2. 将图片上传到 "avatars" 存储桶。
        supabase.storage
            .from(AVATARS_BUCKET)
            .upload(
                path = fileName,
                data = imageBytes,
                upsert = false // 通常不建议覆盖，除非有特定逻辑
            )

        // 3. 获取上传后文件的公开访问URL。
        val newAvatarUrl = supabase.storage.from(AVATARS_BUCKET).publicUrl(fileName)

        // 4. 调用我们新创建的函数，只更新用户的头像URL。
        return updateAvatarUrl(userId, newAvatarUrl)
    }

    /**
     * 只更新指定用户的用户名。
     * @param userId 要更新的用户的ID。
     * @param newUsername 新的用户名。
     * @return 更新成功后的UserProfile对象。
     */
    suspend fun updateUsername(userId: String, newUsername: String): UserProfile {
        val result = supabase.postgrest[PROFILES_TABLE].update(
            {
                set("username", newUsername)
            }
        ) {
            filter {
                eq("id", userId)
            }
            select()
        }.decodeList<UserProfile>()

        if (result.isEmpty()) {
            throw IllegalStateException("Username update failed for user ID: $userId. This is likely due to RLS policies.")
        }
        return result.first()
    }

    /**
     * 只更新指定用户的头像URL。
     * 这个方法主要由 uploadAvatar 内部调用，但也可以在已有URL时直接使用。
     * @param userId 要更新的用户的ID。
     * @param newAvatarUrl 新的头像URL。
     * @return 更新成功后的UserProfile对象。
     */
    suspend fun updateAvatarUrl(userId: String, newAvatarUrl: String): UserProfile {
        val result = supabase.postgrest[PROFILES_TABLE].update(
            {
                set("avatar_url", newAvatarUrl)
            }
        ) {
            filter {
                eq("id", userId)
            }
            select()
        }.decodeList<UserProfile>()

        if (result.isEmpty()) {
            throw IllegalStateException("Avatar URL update failed for user ID: $userId. This is likely due to RLS policies.")
        }
        return result.first()
    }

    /**
     * Updates the FCM token for a given user.
     * @param userId The ID of the user to update.
     * @param token The new FCM token.
     */
    suspend fun updateUserFcmToken(userId: String, token: String) {
        try {
            supabase.postgrest[PROFILES_TABLE].update(
                {
                    // Assuming you have an 'fcm_token' column in your 'users' table
                    set("fcm_token", token)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            Log.d(TAG, "Successfully updated FCM token for user ID: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token for user ID: $userId", e)
            // Re-throw the exception to let the caller know something went wrong.
            throw e
        }
    }


    /**
     * Deletes a post from the database and verifies the deletion.
     * This is done by attempting a selection immediately after the deletion.
     * If the post is still found, it throws an exception, which is often
     * caused by RLS (Row-Level Security) policies.
     * @param post The post to be deleted.
     */
    suspend fun deletePost(post: Post) {
        // Step 1: Attempt to delete the post.
        supabase.postgrest[POST_TABLE].delete {
            filter {
                eq("id", post.id)
            }
        }

        // Step 2: Immediately try to fetch the post we just tried to delete.
        val result = supabase.postgrest[POST_TABLE].select {
            filter {
                eq("id", post.id)
            }
        }.decodeList<Post>()

        // Step 3: If the result list is not empty, the post was not deleted.
        if (result.isNotEmpty()) {
            // Step 4: Throw an exception explaining the likely cause.
            throw IllegalStateException("Deletion failed: The post still exists after deletion attempt. This is likely due to Row-Level Security (RLS) policies. Please check your Supabase dashboard.")
        }
    }

    /**
     * Deletes a comment from the database and verifies the deletion.
     * @param commentId The id of the comment to be deleted.
     */
    suspend fun deleteComment(commentId: Long) {
        // Step 1: Attempt to delete the comment.
        supabase.postgrest[COMMENTS_TABLE].delete {
            filter {
                eq("id", commentId)
            }
        }

        // Step 2: Verify deletion by trying to fetch the comment we just deleted.
        val result = supabase.postgrest[COMMENTS_TABLE].select {
            filter {
                eq("id", commentId)
            }
        }.decodeList<Comment>()

        // Step 3: If the result is not empty, the comment was not deleted.
        if (result.isNotEmpty()) {
            throw IllegalStateException("Deletion failed for comment ID: $commentId. This is likely due to RLS policies. Please check the 'DELETE' policy on the 'comments' table in your Supabase dashboard.")
        }
    }


    /**
     * 上传一张图片到帖子的存储桶中。
     *
     * @param imageBytes 图片的字节数组。
     * @param fileName 包含扩展名的完整文件名 (例如, "some-uuid.jpg")。
     * @return 上传成功后图片的公开访问URL。
     */
    suspend fun uploadPostImage(imageBytes: ByteArray, fileName: String): String {
        // 直接使用传入的文件名进行上传
        supabase.storage
            .from(POST_IMAGES_BUCKET)
            .upload(
                path = fileName,
                data = imageBytes, // 直接传递字节数组
                upsert = false
            )

        // 获取并返回上传后文件的公开URL
        return supabase.storage.from(POST_IMAGES_BUCKET).publicUrl(fileName)
    }

    /**
     * 将给定的图片Uri进行压缩和尺寸调整，同时通过处理EXIF方向来完美保持原始宽高比，
     * 最终转换为适合上传的ByteArray。
     *
     * @param context Context对象，用于访问ContentResolver。
     * @param uri 要压缩的图片的Uri。
     * @param maxDimension 图片最长边的目标尺寸。
     * @param quality 压缩质量 (0-100)。
     * @return 包含压缩后JPEG图片数据的ByteArray。
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1080,
        quality: Int = 75
    ): ByteArray {
        // 使用两个输入流，因为ExifInterface和BitmapFactory.decodeStream都会消耗流
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        var srcWidth = options.outWidth.toFloat()
        var srcHeight = options.outHeight.toFloat()
        if (srcWidth <= 0f || srcHeight <= 0f) return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)

        // --- 根据EXIF方向准备旋转矩阵 ---
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1.0f, 1.0f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.preScale(-1.0f, 1.0f)
            }
            else -> {
                // 不需要旋转
            }
        }

        if (orientation == ExifInterface.ORIENTATION_TRANSPOSE || orientation == ExifInterface.ORIENTATION_TRANSVERSE || orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            // 如果旋转了90或270度，宽高需要互换
            val temp = srcWidth
            srcWidth = srcHeight
            srcHeight = temp
        }

        // --- 计算缩放比例 ---
        var inSampleSize = 1f
        if (srcHeight > maxDimension || srcWidth > maxDimension) {
            inSampleSize = if (srcWidth > srcHeight) {
                srcWidth / maxDimension
            } else {
                srcHeight / maxDimension
            }
        }

        // --- 使用Matrix进行缩放和旋转 ---
        matrix.postScale(1/inSampleSize, 1/inSampleSize)

        val scaledBitmap = context.contentResolver.openInputStream(uri)?.use {
            val sourceBitmap = BitmapFactory.decodeStream(it)
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
        }

        // --- 压缩为JPEG ---
        val outputStream = ByteArrayOutputStream()
        scaledBitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        scaledBitmap?.recycle() // 及时回收Bitmap

        return outputStream.toByteArray()
    }
}
