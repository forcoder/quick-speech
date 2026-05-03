package com.quickspeech.wubi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 五笔词库 Room 数据库
 * 包含编码表和用户词频表
 */
@Database(
    entities = [WubiWordEntry::class, UserFrequencyEntry::class, RecentWordEntry::class],
    version = 1,
    exportSchema = true
)
abstract class WubiDatabase : RoomDatabase() {

    abstract fun wubiDao(): WubiDao

    companion object {
        private const val DATABASE_NAME = "wubi_dict.db"

        fun create(context: Context): WubiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WubiDatabase::class.java,
                DATABASE_NAME
            )
                .createFromAsset("database/wubi_dict.db")
                .build()
        }
    }
}
