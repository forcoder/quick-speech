package com.quickspeech.common.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.quickspeech.common.db.dao.UserStyleDao
import com.quickspeech.common.db.dao.KnowledgeEditDao
import com.quickspeech.common.db.entity.UserStyleProfile
import com.quickspeech.common.db.entity.KnowledgeEditEntry
import com.quickspeech.common.db.entity.InteractionLog

@Database(
    entities = [
        UserStyleProfile::class,
        KnowledgeEditEntry::class,
        InteractionLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userStyleDao(): UserStyleDao
    abstract fun knowledgeEditDao(): KnowledgeEditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickspeech_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
