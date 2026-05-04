package com.quickspeech.wubi

import com.quickspeech.wubi.engine.WubiEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WubiModule {

    @Provides
    @Singleton
    fun provideWubiEngine(): WubiEngine {
        return WubiEngine()
    }
}
