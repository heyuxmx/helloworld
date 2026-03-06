package com.heyu.zhudeapp.di

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.heyu.zhudeapp.util.VideoUtils
import com.heyu.zhudeapp.util.VideoCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object UploadManager {

    sealed class UploadState {
        object Idle : UploadState()
        data class Uploading(val message: String) : UploadState()
        object Success : UploadState()
        data class Failure(val message: String) : UploadState()
    }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    private lateinit var appScope: CoroutineScope

    fun init(scope: CoroutineScope) {
        appScope = scope
    }

    @OptIn(UnstableApi::class)
    fun enqueue(
        context: Context,
        content: String,
        uris: List<Uri>,
        userId: String
    ) {
        appScope.launch(Dispatchers.IO) {
            try {
                val total = uris.size
                val completed = AtomicInteger(0)
                _state.value = UploadState.Uploading("正在处理媒体文件...")

                val urls = uris.map { uri ->
                    val mimeType = context.contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video/") == true
                    val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"

                    val fileBytes = if (isVideo) {
                        _state.value = UploadState.Uploading("正在压缩视频...")
                        val compressed = VideoUtils.compressVideoIfNeeded(context, uri) {}
                        VideoUtils.uriToByteArrayWithLimit(context, compressed)
                    } else {
                        HeyuModule.compressImage(context, uri)
                    }

                    val done = completed.incrementAndGet()
                    _state.value = UploadState.Uploading("正在上传 ($done/$total)...")

                    val url = if (isVideo) {
                        HeyuModule.uploadPostVideo(fileBytes, fileName)
                    } else {
                        HeyuModule.uploadPostImage(fileBytes, fileName)
                    }

                    if (isVideo) {
                        VideoCacheManager.saveOriginalVideoToLibrary(context, url, uri)
                    }
                    url
                }

                _state.value = UploadState.Uploading("正在保存动态...")
                HeyuModule.createPost(content, urls, userId)
                _state.value = UploadState.Success
            } catch (e: Exception) {
                _state.value = UploadState.Failure(e.localizedMessage ?: "上传失败")
            }
        }
    }

    fun resetToIdle() {
        _state.value = UploadState.Idle
    }
}
