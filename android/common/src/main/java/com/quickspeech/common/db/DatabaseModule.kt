package com.quickspeech.common.db

import android.content.Context
import com.quickspeech.common.db.dao.KnowledgeEditDao
import com.quickspeech.common.db.dao.UserStyleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserStyleDao(database: AppDatabase): UserStyleDao {
        return database.userStyleDao()
    }

    @Provides
    fun provideKnowledgeEditDao(database: AppDatabase): KnowledgeEditDao {
        return database.knowledgeEditDao()
    }

    @Provides
    fun provideEditHistoryDao(database: AppDatabase): EditHistoryDao {
        return database.editHistoryDao()
    }

    @Provides
    fun provideCorrectionDao(database: AppDatabase): CorrectionDao {
        return database.correctionDao()
    }

    @Provides
    fun provideBehaviorRecordDao(database: AppDatabase): BehaviorRecordDao {
        return database.behaviorRecordDao()
    }

    @Provides
    fun provideStyleProfileDao(database: AppDatabase): StyleProfileDao {
        return database.styleProfileDao()
    }
}
