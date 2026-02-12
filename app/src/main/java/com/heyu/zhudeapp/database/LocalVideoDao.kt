package com.heyu.zhudeapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heyu.zhudeapp.data.LocalVideo

@Dao
interface LocalVideoDao {
    @Query("SELECT * FROM local_videos WHERE videoUrl = :url")
    suspend fun getLocalVideoByUrl(url: String): LocalVideo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalVideo(localVideo: LocalVideo)

    @Query("DELETE FROM local_videos WHERE videoUrl = :url")
    suspend fun deleteByUrl(url: String)
}
