package com.quickspeech.common.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: StyleProfileEntity)

    @Update
    suspend fun update(profile: StyleProfileEntity)

    @Query("SELECT * FROM style_profiles WHERE id = :id")
    fun getProfile(id: String = "default"): Flow<StyleProfileEntity?>

    @Query("SELECT * FROM style_profiles WHERE id = :id")
    suspend fun getProfileSync(id: String = "default"): StyleProfileEntity?

    @Query("DELETE FROM style_profiles WHERE id = :id")
    suspend fun delete(id: String = "default")

    @Query("DELETE FROM style_profiles")
    suspend fun deleteAll()
}
