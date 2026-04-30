package com.quickspeech.common.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "style_profiles")
data class StyleProfileEntity(
    @PrimaryKey
    val id: String = "default",
    val formalityLevel: Int = 5,
    val concisenessLevel: Int = 5,
    val commonPhrases: List<String> = emptyList(),
    val sentencePatterns: List<String> = emptyList(),
    val punctuationHabits: List<String> = emptyList(),
    val emailStyle: String = "neutral",
    val imStyle: String = "casual",
    val documentStyle: String = "formal",
    val lastUpdated: Long = System.currentTimeMillis(),
    val totalSamples: Int = 0
)
