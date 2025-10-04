package com.heyu.zhudeapp.di

import com.heyu.zhudeapp.data.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
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
     * @param fileExtension 文件扩展名 (例如, "jpg", "png")。
     * @return 上传成功后图片的公开访问URL。
     */
    suspend fun uploadPostImage(imageBytes: ByteArray, fileExtension: String): String {
        // 1. 生成一个独一无二的文件名，避免文件覆盖
        val fileName = "${UUID.randomUUID()}.$fileExtension"

        // 2. 最终的、唯一的正确方法：
        //    直接调用接收 ByteArray 的 upload 函数。我们不能手动设置 contentType，
        //    但 Supabase 会根据你提供的文件扩展名（.jpg, .png等）自动推断正确的内容类型。
        supabase.storage
            .from(POST_IMAGES_BUCKET)
            .upload(
                path = fileName,
                data = imageBytes, // 直接传递字节数组
                upsert = false
            )

        // 3. 获取并返回上传后文件的公开URL
        return supabase.storage.from(POST_IMAGES_BUCKET).publicUrl(fileName)
    }

}
