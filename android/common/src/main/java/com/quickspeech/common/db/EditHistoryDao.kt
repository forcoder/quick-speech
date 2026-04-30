package com.quickspeech.common.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EditHistoryDao {

    @Insert
    suspend fun insert(history: EditHistoryEntity): Long

    @Query("SELECT * FROM edit_history ORDER BY editTimestamp DESC")
    fun getAllHistory(): Flow<List<EditHistoryEntity>>

    @Query("SELECT * FROM edit_history WHERE knowledgeId = :knowledgeId ORDER BY editTimestamp DESC")
    fun getHistoryByKnowledgeId(knowledgeId: String): Flow<List<EditHistoryEntity>>

    @Query("SELECT * FROM edit_history WHERE knowledgeId = :knowledgeId ORDER BY editTimestamp DESC LIMIT 1")
    suspend fun getLatestEdit(knowledgeId: String): EditHistoryEntity?

    @Query("DELETE FROM edit_history WHERE knowledgeId = :knowledgeId")
    suspend fun deleteByKnowledgeId(knowledgeId: String)

    @Delete
    suspend fun delete(history: EditHistoryEntity)

    @Query("DELETE FROM edit_history")
    suspend fun deleteAll()
}
