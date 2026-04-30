package com.quickspeech.common.db

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEditHistoryDao(provider: DatabaseProvider): EditHistoryDao = provider.editHistoryDao

    @Provides
    @Singleton
    fun provideCorrectionDao(provider: DatabaseProvider): CorrectionDao = provider.correctionDao

    @Provides
    @Singleton
    fun provideBehaviorRecordDao(provider: DatabaseProvider): BehaviorRecordDao = provider.behaviorRecordDao

    @Provides
    @Singleton
    fun provideStyleProfileDao(provider: DatabaseProvider): StyleProfileDao = provider.styleProfileDao
}
