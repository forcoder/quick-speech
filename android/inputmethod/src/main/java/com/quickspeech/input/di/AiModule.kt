package com.quickspeech.input.di

import android.content.Context
import com.quickspeech.input.ai.BehaviorRecorder
import com.quickspeech.input.ai.data.*
import com.quickspeech.input.ai.engine.LocalReplyGenerator
import com.quickspeech.input.ai.engine.ReplyContextAnalyzer
import com.quickspeech.input.ai.engine.StyleAdapter
import com.quickspeech.input.ai.engine.StyleAnalyzer
import com.quickspeech.input.ai.engine.StyleLearningEngine
import com.quickspeech.input.ai.network.AiReplyApi
import com.quickspeech.input.ai.network.AiReplyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiReplyDatabase(@ApplicationContext context: Context): AiReplyDatabase {
        return AiReplyDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserFeedbackDao(database: AiReplyDatabase): UserFeedbackDao {
        return database.userFeedbackDao()
    }

    @Provides
    @Singleton
    fun provideAppCategoryDao(database: AiReplyDatabase): AppCategoryDao {
        return database.appCategoryDao()
    }

    @Provides
    @Singleton
    fun provideAiReplyApi(okHttpClient: OkHttpClient): AiReplyApi {
        return Retrofit.Builder()
            .baseUrl(AiReplyApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiReplyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLocalReplyGenerator(): LocalReplyGenerator {
        return LocalReplyGenerator()
    }

    @Provides
    @Singleton
    fun provideReplyContextAnalyzer(): ReplyContextAnalyzer {
        return ReplyContextAnalyzer()
    }

    @Provides
    @Singleton
    fun provideAiReplyRepository(
        api: AiReplyApi,
        preferencesRepository: PreferencesRepository,
        feedbackDao: UserFeedbackDao,
        localReplyGenerator: LocalReplyGenerator,
        contextAnalyzer: ReplyContextAnalyzer
    ): AiReplyRepository {
        return AiReplyRepository(api, preferencesRepository, feedbackDao, localReplyGenerator, contextAnalyzer)
    }

    @Provides
    @Singleton
    fun provideStyleAdapter(): StyleAdapter {
        return StyleAdapter()
    }

    @Provides
    @Singleton
    fun provideStyleLearningEngine(
        behaviorRecorder: BehaviorRecorder,
        styleAnalyzer: StyleAnalyzer,
        styleAdapter: StyleAdapter
    ): StyleLearningEngine {
        return StyleLearningEngine(behaviorRecorder, styleAnalyzer, styleAdapter)
    }
}