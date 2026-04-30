package com.quickspeech.input.di

import android.content.Context
import androidx.room.Room
import com.quickspeech.input.ai.data.*
import com.quickspeech.input.ai.network.AiReplyApi
import com.quickspeech.input.ai.network.AiReplyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiReplyDatabase(@ApplicationContext context: Context): AiReplyDatabase {
        return Room.databaseBuilder(
            context,
            AiReplyDatabase::class.java,
            "ai_reply_database"
        ).build()
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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
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
    fun provideAiReplyRepository(
        api: AiReplyApi,
        preferencesRepository: PreferencesRepository,
        feedbackDao: UserFeedbackDao
    ): AiReplyRepository {
        return AiReplyRepository(api, preferencesRepository, feedbackDao)
    }
}
