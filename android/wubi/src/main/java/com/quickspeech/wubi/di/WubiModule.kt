package com.quickspeech.wubi.di

import android.content.Context
import com.quickspeech.wubi.data.CloudSyncManager
import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiDatabase
import com.quickspeech.wubi.engine.AssociativeEngine
import com.quickspeech.wubi.engine.FrequencyLearner
import com.quickspeech.wubi.engine.WubiInputEngine
import com.quickspeech.wubi.engine.WubiMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 五笔模块 Hilt 依赖注入配置
 * 注意：WubiEngine（JNI）由顶层 WubiModule 提供
 */
@Module
@InstallIn(SingletonComponent::class)
object WubiHiltModule {

    @Provides
    @Singleton
    fun provideWubiDatabase(@ApplicationContext context: Context): WubiDatabase {
        return WubiDatabase.create(context)
    }

    @Provides
    fun provideWubiDao(database: WubiDatabase): WubiDao {
        return database.wubiDao()
    }

    @Provides
    fun provideWubiMatcher(dao: WubiDao): WubiMatcher {
        return WubiMatcher(dao)
    }

    @Provides
    fun provideAssociativeEngine(dao: WubiDao): AssociativeEngine {
        return AssociativeEngine(dao)
    }

    @Provides
    fun provideFrequencyLearner(dao: WubiDao): FrequencyLearner {
        return FrequencyLearner(dao)
    }

    @Provides
    @Singleton
    fun provideCloudSyncManager(
        @ApplicationContext context: Context,
        dao: WubiDao
    ): CloudSyncManager {
        return CloudSyncManager(context, dao)
    }
}
