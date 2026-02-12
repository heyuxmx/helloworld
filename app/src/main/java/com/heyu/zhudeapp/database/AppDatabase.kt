package com.heyu.zhudeapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.data.LocalVideo

@Database(entities = [Post::class, LocalVideo::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun localVideoDao(): LocalVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zhude_database"
                )
                .fallbackToDestructiveMigration() // 傻逼，版本升级直接删表，省得迁移报错
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
