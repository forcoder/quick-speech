package com.quickspeech.common.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickspeech.common.db.entity.KnowledgeEditEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeEditDao {
    @Query("SELECT * FROM knowledge_edit_entry ORDER BY editTime DESC")
    fun getAllEdits(): Flow<List<KnowledgeEditEntry>>

    @Query("SELECT * FROM knowledge_edit_entry WHERE originalContentId = :contentId")
    fun getEditsForContent(contentId: String): Flow<List<KnowledgeEditEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edit: KnowledgeEditEntry)

    @Update
    suspend fun update(edit: KnowledgeEditEntry)

    @Query("DELETE FROM knowledge_edit_entry WHERE id = :id")
    suspend fun deleteById(id: String)
}
