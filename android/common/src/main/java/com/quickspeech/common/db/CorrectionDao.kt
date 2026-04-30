package com.quickspeech.common.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrectionDao {

    @Insert
    suspend fun insert(correction: CorrectionEntity): Long

    @Update
    suspend fun update(correction: CorrectionEntity)

    @Query("SELECT * FROM corrections ORDER BY correctionTimestamp DESC")
    fun getAllCorrections(): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE status = :status ORDER BY correctionTimestamp DESC")
    fun getCorrectionsByStatus(status: CorrectionStatus): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE knowledgeId = :knowledgeId ORDER BY correctionTimestamp DESC")
    fun getCorrectionsByKnowledgeId(knowledgeId: String): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE status = 'PENDING' ORDER BY correctionTimestamp ASC")
    suspend fun getPendingCorrections(): List<CorrectionEntity>

    @Delete
    suspend fun delete(correction: CorrectionEntity)

    @Query("DELETE FROM corrections")
    suspend fun deleteAll()
}
