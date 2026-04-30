package com.quickspeech.common.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_style_profile")
data class UserStyleProfile(
    @PrimaryKey val id: Int = 1,
    val formalityScore: Int = 5,
    val concisenessScore: Int = 5,
    val useHonorifics: Boolean = true,
    val includeExplanations: Boolean = true,
    val preferredLength: Int = 50,
    val structurePreference: String = "paragraph",
    val commonPhrases: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_edit_entry")
data class KnowledgeEditEntry(
    @PrimaryKey val id: String,
    val originalContentId: String,
    val originalContent: String,
    val correctedContent: String,
    val editReason: String? = null,
    val mergeStatus: String = "pending",
    val editTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "interaction_log")
data class InteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val content: String,
    val aiReplyId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
