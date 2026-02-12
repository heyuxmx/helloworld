package com.heyu.zhudeapp.di

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 1GB disk cache - Trading space for time
        val diskCacheSizeBytes = 1024 * 1024 * 1024L 
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
        
        // Increase memory cache size to 50MB (default is usually smaller)
        builder.setMemoryCache(LruResourceCache(50 * 1024 * 1024))

        // Default request options
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache all versions of the image
                .dontAnimate() // Faster display
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // Save memory, faster decoding (less quality but faster)
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
