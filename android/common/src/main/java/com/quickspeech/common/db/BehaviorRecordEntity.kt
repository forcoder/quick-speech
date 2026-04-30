package com.quickspeech.common.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_records")
data class BehaviorRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalReply: String,
    val userAction: UserActionType,
    val modifiedReply: String? = null,
    val selfWrittenReply: String? = null,
    val sceneType: String = "general",
    val timestamp: Long = System.currentTimeMillis(),
    val contextPrompt: String? = null
)

enum class UserActionType {
    ACCEPTED,
    SKIPPED,
    MODIFIED,
    SELF_WRITTEN
}
