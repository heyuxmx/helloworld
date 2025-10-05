package com.heyu.zhudeapp.di

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.heyu.zhudeapp.data.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 全局Supabase模块，提供一个单例的SupabaseClient实例，并封装了常用的功能函数。
 */
object SupabaseModule {

    private const val POST_TABLE = "posts"
    private const val POST_IMAGES_BUCKET = "post-images"

    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://bvgtzgxscnqhugjirgzp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2Z3R6Z3hzY25xaHVnamlyZ3pwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MDA5NTYsImV4cCI6MjA3NTA3Njk1Nn0.bSF7FkLgvFwsJOODgG8AKtLBpF-OPyzaUfoWSUmoFes"
    ) {
        install(Postgrest)
        install(Storage)
    }

    /**
     * 从数据库获取所有动态的列表，并严格按照创建时间降序排列。
     * @return 从新到旧排序的动态列表。
     */
    suspend fun getPosts(): List<Post> {
        return supabase.postgrest[POST_TABLE].select {
            order("created_at", Order.DESCENDING)
        }.decodeList<Post>()
    }

    /**
     * 创建一条新的动态，并验证插入是否成功。
     * 如果插入失败（通常因为RLS策略），会抛出异常。
     * @param content 动态的文本内容。
     * @param imageUrls 可选的图片URL列表。
     * @return 创建成功并从数据库返回的Post对象。
     */
    suspend fun createPost(content: String, imageUrls: List<String> = emptyList()): Post {
        val newPost = Post(
            content = content,
            imageUrls = imageUrls
            // 备注: userId, username 等字段未来可以和用户认证流程结合
        )

        // 步骤 1: 插入数据并请求返回插入的记录
        val result = supabase.postgrest[POST_TABLE].insert(newPost) {
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
        }

        // 对于旋转90或270度的图片，其宽高在计算缩放比例时需要交换
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270 || orientation == ExifInterface.ORIENTATION_TRANSPOSE || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            val temp = srcWidth
            srcWidth = srcHeight
            srcHeight = temp
        }

        // --- 计算缩放比例 ---
        var scale = 1.0f
        if (srcWidth > maxDimension || srcHeight > maxDimension) {
            scale = if (srcWidth > srcHeight) {
                maxDimension / srcWidth
            } else {
                maxDimension / srcHeight
            }
        }
        matrix.postScale(scale, scale)

        // --- 解码、应用变换并压缩 ---
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return ByteArray(0)

        // 使用矩阵一次性完成旋转和缩放
        val transformedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        if (transformedBitmap != originalBitmap) {
            originalBitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()
        transformedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        transformedBitmap.recycle()

        return outputStream.toByteArray()
    }
}
