package com.quickspeech.common.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "behavior_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["userAction"]),
        Index(value = ["sceneType"])
    ]
)
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
