package com.heyu.zhudeapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.heyu.zhudeapp.data.LocalVideo
import com.heyu.zhudeapp.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@UnstableApi
object VideoCacheManager {
    private var simpleCache: SimpleCache? = null
    private const val CACHE_SIZE = 500 * 1024 * 1024L 
    private const val TAG = "VideoCacheManager"

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            simpleCache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(CACHE_SIZE), StandaloneDatabaseProvider(context))
        }
        return simpleCache!!
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * 获取视频播放地址：优先返回本地库的原像素视频路径
     */
    suspend fun getVideoPlayUri(context: Context, remoteUrl: String): Uri {
        val localVideoDao = AppDatabase.getDatabase(context).localVideoDao()
        val localVideo = localVideoDao.getLocalVideoByUrl(remoteUrl)
        
        if (localVideo != null) {
            val file = File(localVideo.localPath)
            if (file.exists()) {
                Log.d(TAG, "发现本地视频库文件: ${localVideo.localPath}")
                return Uri.fromFile(file)
            } else {
                // 如果数据库记录存在但文件被删了，清理掉记录
                localVideoDao.deleteByUrl(remoteUrl)
            }
        }
        
        // 没找到本地库，开启后台下载同步，返回远程地址
        GlobalScope.launch(Dispatchers.IO) {
            downloadAndSaveToLocalLibrary(context, remoteUrl)
        }
        return Uri.parse(remoteUrl)
    }

    /**
     * 将原像素视频保存到本地库
     */
    suspend fun saveOriginalVideoToLibrary(context: Context, remoteUrl: String, originalUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val libDir = File(context.filesDir, "original_videos")
                if (!libDir.exists()) libDir.mkdirs()
                
                val fileName = "orig_${System.currentTimeMillis()}.mp4"
                val destFile = File(libDir, fileName)
                
                context.contentResolver.openInputStream(originalUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (destFile.exists()) {
                    AppDatabase.getDatabase(context).localVideoDao().insertLocalVideo(
                        LocalVideo(remoteUrl, destFile.absolutePath, isOriginal = true)
                    )
                    Log.d(TAG, "原像素视频已存入本地库: ${destFile.absolutePath}")
                }
                Unit // 显式返回 Unit 避免 if 表达式报错
            } catch (e: Exception) {
                Log.e(TAG, "保存原视频失败", e)
            }
        }
    }

    /**
     * 从远程下载视频到本地库（非缓存，是永久存储）
     */
    private suspend fun downloadAndSaveToLocalLibrary(context: Context, remoteUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val libDir = File(context.filesDir, "downloaded_videos")
                if (!libDir.exists()) libDir.mkdirs()
                
                val fileName = "down_${remoteUrl.hashCode()}.mp4"
                val destFile = File(libDir, fileName)
                
                if (destFile.exists()) return@withContext

                Log.d(TAG, "开始下载视频到本地库: $remoteUrl")
                URL(remoteUrl).openStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                AppDatabase.getDatabase(context).localVideoDao().insertLocalVideo(
                    LocalVideo(remoteUrl, destFile.absolutePath, isOriginal = false)
                )
                Log.d(TAG, "视频已下载并存入本地库")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "视频同步本地库失败", e)
            }
        }
    }

    fun preCacheVideo(context: Context, videoUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // 如果本地库已经有了，就不浪费带宽预取缓存了
                val localVideo = AppDatabase.getDatabase(context).localVideoDao().getLocalVideoByUrl(videoUrl)
                if (localVideo != null && File(localVideo.localPath).exists()) return@launch

                val dataSpec = DataSpec(Uri.parse(videoUrl), 0, 1024 * 1024 * 2) 
                val cacheWriter = CacheWriter(getCacheDataSourceFactory(context).createDataSource(), dataSpec, null, null)
                cacheWriter.cache()
            } catch (e: Exception) {}
        }
    }
}
