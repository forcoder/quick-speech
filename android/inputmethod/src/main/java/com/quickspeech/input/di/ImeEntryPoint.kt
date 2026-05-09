package com.quickspeech.input.di

import com.quickspeech.common.network.ApiService
import com.quickspeech.input.ai.network.AiReplyRepository
import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.engine.UserRuleEngine
import com.quickspeech.wubi.engine.WubiInputEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImeEntryPoint {
    fun aiReplyRepository(): AiReplyRepository
    fun apiService(): ApiService
    fun wubiInputEngine(): WubiInputEngine
    fun userRuleEngine(): UserRuleEngine
    fun userRuleDao(): UserRuleDao
}
