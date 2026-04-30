package com.quickspeech.common.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorRecordDao {

    @Insert
    suspend fun insert(record: BehaviorRecordEntity): Long

    @Query("SELECT * FROM behavior_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BehaviorRecordEntity>>

    @Query("SELECT * FROM behavior_records WHERE userAction = :action ORDER BY timestamp DESC")
    fun getRecordsByAction(action: UserActionType): Flow<List<BehaviorRecordEntity>>

    @Query("SELECT * FROM behavior_records WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecordsSince(since: Long): Flow<List<BehaviorRecordEntity>>

    @Query("SELECT * FROM behavior_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<BehaviorRecordEntity>

    @Query("SELECT COUNT(*) FROM behavior_records")
    suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM behavior_records WHERE userAction = :action")
    suspend fun getCountByAction(action: UserActionType): Int

    @Delete
    suspend fun delete(record: BehaviorRecordEntity)

    @Query("DELETE FROM behavior_records")
    suspend fun deleteAll()

    @Query("DELETE FROM behavior_records WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
