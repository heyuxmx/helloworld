package com.heyu.zhudeapp.di

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.heyu.zhudeapp.data.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
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
     * 从数据库获取所有动态的列表。
     * @return 动态列表。
     */
    suspend fun getPosts(): List<Post> {
        return supabase.postgrest[POST_TABLE].select().decodeList<Post>()
    }

    /**
     * 创建一条新的动态。
     * @param content 动态的文本内容。
     * @param imageUrls 可选的图片URL列表。
     */
    suspend fun createPost(content: String, imageUrls: List<String> = emptyList()) {
        val newPost = Post(
            content = content,
            imageUrls = imageUrls
            // 备注: userId, username 等字段未来可以和用户认证流程结合
        )
        supabase.postgrest[POST_TABLE].insert(newPost)
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
     * 将给定的图片Uri进行压缩和尺寸调整，转换为适合上传的ByteArray。
     *
     * @param context Context对象，用于访问ContentResolver。
     * @param uri 要压缩的图片的Uri。
     * @param maxWidth 调整后的图片最大宽度。
     * @param maxHeight 调整后的图片最大高度。
     * @param quality 压缩质量 (0-100)。
     * @return 包含压缩后JPEG图片数据的ByteArray。
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        maxHeight: Int = 1920,
        quality: Int = 80
    ): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            // 首先，只解码边界，不加载整个图片，以获取原始尺寸
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // 计算缩放比例
        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        val scaleFactor = maxOf(1, minOf(srcWidth / maxWidth, srcHeight / maxHeight))

        // 使用计算出的缩放比例来真正地解码、缩放图片
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scaleFactor
        }
        val bitmap = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, decodeOptions)!!
        }

        // 将缩放后的Bitmap压缩为JPEG格式的ByteArray
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
            it.toByteArray()
        }
    }
}
