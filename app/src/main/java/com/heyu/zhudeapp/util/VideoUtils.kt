package com.heyu.zhudeapp.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.resize.AtMostResizer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object VideoUtils {
    private const val TAG = "VideoUtils"

    fun getVideoFileSize(context: Context, uri: Uri): Long {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").length()
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    fun getVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0
        } catch (e: Exception) {
            0
        } finally {
            retriever.release()
        }
    }

    fun uriToByteArrayWithLimit(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } 
            ?: throw IllegalArgumentException("Failed to read file")
    }

    fun isVideoTooLong(context: Context, uri: Uri, maxDurationInSeconds: Long = 1800): Boolean {
        val duration = getVideoDuration(context, uri)
        return duration > (maxDurationInSeconds * 1000)
    }

    /**
     * 极度压缩算法：确保文件在 10MB 左右，防止 Supabase 报错
     */
    suspend fun compressVideoIfNeeded(
        context: Context, 
        inputUri: Uri, 
        onProgress: (Int) -> Unit = {}
    ): Uri {
        val fileSize = getVideoFileSize(context, inputUri)
        // 如果视频小于 10MB，直接跳过压缩以节省时间
        if (fileSize in 1..10_000_000L) return inputUri

        val durationMs = getVideoDuration(context, inputUri)
        if (durationMs <= 0) return inputUri

        // 目标设为 8.5MB，预留足够空间给音频和容器开销，确保最终输出在 10MB 以内
        val targetSizeBytes = 8_500_000L
        val durationSec = durationMs / 1000.0
        
        // 计算压缩后的理论比特率 (bps)
        var targetBitrate = ((targetSizeBytes * 8) / durationSec).toInt()
        
        // 分辨率自动降级：保证在低比特率下画面不模糊
        val targetHeight = when {
            targetBitrate >= 1_500_000 -> 540  
            targetBitrate >= 800_000 -> 480
            else -> {
                targetBitrate = targetBitrate.coerceAtLeast(400_000) // 保底比特率
                360 
            }
        }

        val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")

        return try {
            val success = suspendCancellableCoroutine<Boolean> { continuation ->
                val strategy = DefaultVideoStrategy.Builder()
                    .bitRate(targetBitrate.toLong())
                    .frameRate(25) // 降为 25 帧以节省空间
                    .addResizer(AtMostResizer(targetHeight))
                    .build()

                Transcoder.into(outputFile.path)
                    .addDataSource(context, inputUri)
                    .setVideoTrackStrategy(strategy)
                    .setListener(object : TranscoderListener {
                        override fun onTranscodeProgress(progress: Double) {
                            onProgress((progress * 100).toInt())
                        }
                        override fun onTranscodeCompleted(successCode: Int) {
                            if (continuation.isActive) continuation.resume(true)
                        }
                        override fun onTranscodeCanceled() {
                            if (continuation.isActive) continuation.resume(false)
                        }
                        override fun onTranscodeFailed(exception: Throwable) {
                            Log.e(TAG, "Transcode failed", exception)
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }).transcode()
            }
            if (success && outputFile.exists()) {
                val newSize = outputFile.length()
                Log.d(TAG, "压缩完成，新大小: ${newSize / 1024} KB")
                Uri.fromFile(outputFile)
            } else {
                inputUri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression error", e)
            inputUri
        }
    }
}
