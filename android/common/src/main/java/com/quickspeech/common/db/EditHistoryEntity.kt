package com.quickspeech.common.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edit_history")
data class EditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val knowledgeId: String,
    val originalContent: String,
    val editedContent: String,
    val editTimestamp: Long = System.currentTimeMillis(),
    val editReason: String? = null
)
