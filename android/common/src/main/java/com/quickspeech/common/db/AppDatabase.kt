package com.quickspeech.common.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quickspeech.common.db.dao.KnowledgeEditDao
import com.quickspeech.common.db.dao.UserStyleDao
import com.quickspeech.common.db.entity.InteractionLog
import com.quickspeech.common.db.entity.KnowledgeEditEntry
import com.quickspeech.common.db.entity.UserStyleProfile

@Database(
    entities = [
        UserStyleProfile::class,
        KnowledgeEditEntry::class,
        InteractionLog::class,
        EditHistoryEntity::class,
        CorrectionEntity::class,
        BehaviorRecordEntity::class,
        StyleProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userStyleDao(): UserStyleDao
    abstract fun knowledgeEditDao(): KnowledgeEditDao

    abstract fun editHistoryDao(): EditHistoryDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun behaviorRecordDao(): BehaviorRecordDao
    abstract fun styleProfileDao(): StyleProfileDao

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
