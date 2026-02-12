package com.heyu.zhudeapp.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
object VideoCacheManager {
    private var simpleCache: SimpleCache? = null
    private const val CACHE_SIZE = 500 * 1024 * 1024L // 500MB video cache

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
     * Pre-cache the first part of a video to allow immediate playback
     */
    fun preCacheVideo(context: Context, videoUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val dataSpec = DataSpec(Uri.parse(videoUrl), 0, 1024 * 1024 * 2) // Cache first 2MB
                val cacheWriter = CacheWriter(
                    getCacheDataSourceFactory(context).createDataSource(),
                    dataSpec,
                    null,
                    null
                )
                cacheWriter.cache()
            } catch (e: Exception) {
                // Ignore pre-cache errors
            }
        }
    }
}
