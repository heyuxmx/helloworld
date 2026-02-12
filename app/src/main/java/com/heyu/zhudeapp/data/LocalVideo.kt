package com.heyu.zhudeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记录视频远程 URL 与本地物理路径的映射关系
 */
@Entity(tableName = "local_videos")
data class LocalVideo(
    @PrimaryKey
    val videoUrl: String,
    val localPath: String,
    val isOriginal: Boolean = false // 是否为原像素视频
)
