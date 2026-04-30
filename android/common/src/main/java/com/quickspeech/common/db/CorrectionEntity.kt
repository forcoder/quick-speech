package com.quickspeech.common.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "corrections")
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val knowledgeId: String,
    val originalContent: String,
    val correctedContent: String,
    val correctionTimestamp: Long = System.currentTimeMillis(),
    val status: CorrectionStatus = CorrectionStatus.PENDING,
    val reviewComment: String? = null
)

enum class CorrectionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
