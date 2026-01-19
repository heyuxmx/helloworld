package com.heyu.zhudeapp.di

import android.content.Context
import android.net.Uri
import android.util.Log
import com.alibaba.sdk.android.oss.*
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 阿里云 OSS 上传管理器
 * 用于处理大视频文件的上传，突破 Supabase 50MB 限制
 */
object OSSManager {
    // 请在阿里云后台确认您的 Bucket 名称 and Endpoint (区域地址)
    private const val BUCKET_NAME = "zhudeapp"
    private const val ENDPOINT = "oss-cn-hangzhou.aliyuncs.com" 
    private const val TAG = "OSSManager"

    /**
     * 上传视频到阿里云
     * @param context 上下文
     * @param stsId 临时访问密钥 ID
     * @param stsSecret 临时访问密钥 Secret
     * @param stsToken 安全令牌
     * @param expiration 过期时间
     * @param videoUri 视频文件的本地 Uri
     * @return 返回上传成功后的视频网络访问链接
     */
    suspend fun uploadVideo(
        context: Context,
        stsId: String,
        stsSecret: String,
        stsToken: String,
        expiration: String,
        videoUri: Uri
    ): String = suspendCancellableCoroutine { continuation ->

        // 1. 配置临时凭证提供者
        val credentialProvider = object : OSSFederationCredentialProvider() {
            override fun getFederationToken(): OSSFederationToken {
                return OSSFederationToken(stsId, stsSecret, stsToken, expiration)
            }
        }

        // 2. 初始化 OSS 客户端
        val conf = ClientConfiguration()
        conf.connectionTimeout = 15 * 1000 // 连接超时，默认15秒
        conf.socketTimeout = 15 * 1000 // socket超时，默认15秒
        conf.maxErrorRetry = 2 // 最大重试次数，默认2次
        
        val oss = OSSClient(context, ENDPOINT, credentialProvider, conf)

        // 3. 生成唯一的文件名，防止覆盖
        val fileName = "videos/${UUID.randomUUID()}.mp4"

        // 4. 创建上传请求
        val request = PutObjectRequest(BUCKET_NAME, fileName, videoUri)

        // 设置上传进度回调
        request.progressCallback = OSSProgressCallback<PutObjectRequest> { _, currentSize, totalSize ->
            if (totalSize > 0) {
                val progress = (100.0 * currentSize / totalSize).toInt()
                Log.d(TAG, "上传进度: $progress%")
            }
        }

        // 5. 执行异步上传
        oss.asyncPutObject(request, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
            override fun onSuccess(p0: PutObjectRequest?, p1: PutObjectResult?) {
                // 拼接最终的可访问 URL
                val videoUrl = "https://$BUCKET_NAME.$ENDPOINT/$fileName"
                Log.d(TAG, "视频上传成功: $videoUrl")
                continuation.resume(videoUrl)
            }

            override fun onFailure(p0: PutObjectRequest?, clientEx: ClientException?, serviceEx: ServiceException?) {
                val errorMsg = serviceEx?.rawMessage ?: clientEx?.message ?: "未知错误"
                Log.e(TAG, "视频上传失败: $errorMsg")
                continuation.resumeWithException(clientEx ?: Exception(errorMsg))
            }
        })
    }
}
