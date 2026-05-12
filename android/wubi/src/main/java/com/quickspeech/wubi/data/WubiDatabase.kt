package com.quickspeech.wubi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WubiWordEntry::class, UserFrequencyEntry::class, RecentWordEntry::class, UserRuleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WubiDatabase : RoomDatabase() {

    abstract fun wubiDao(): WubiDao

    abstract fun userRuleDao(): UserRuleDao

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
