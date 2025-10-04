package com.heyu.zhudeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {

    /**
     * 获取所有动态的列表。
     * @return 一个包含Post列表的Flow，可以被UI层收集。
     */
    fun fetchAllPosts(): Flow<List<Post>> = flow {
        // 直接调用 SupabaseModule 中的高级函数，代码更简洁
        emit(SupabaseModule.getPosts())
    }

    /**
     * 创建一个新的纯文本动态。
     * @param content 动态的文本内容。
     */
    fun createTextPost(content: String) {
        viewModelScope.launch {
            // 调用 SupabaseModule 中的高级函数
            SupabaseModule.createPost(content, null)
        }
    }

    /**
     * 创建一个带图片的新动态。
     * 它会先上传图片，然后将返回的URL与文本内容一起存入数据库。
     * @param content 动态的文本内容。
     * @param imageBytes 图片的字节数组。
     * @param fileExtension 图片的文件扩展名 (e.g., "png", "jpg").
     */
    fun createPostWithImage(content: String, imageBytes: ByteArray, fileExtension: String) {
        viewModelScope.launch {
            // 1. 通过模块上传图片，获取URL
            val imageUrl = SupabaseModule.uploadPostImage(imageBytes, fileExtension)
            // 2. 通过模块创建带有图片URL的帖子
            SupabaseModule.createPost(content, imageUrl)
        }
    }
}
