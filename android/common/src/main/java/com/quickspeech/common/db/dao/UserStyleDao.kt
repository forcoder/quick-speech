package com.quickspeech.common.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickspeech.common.db.entity.UserStyleProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStyleDao {
    @Query("SELECT * FROM user_style_profile WHERE id = 1")
    fun getStyleProfile(): Flow<UserStyleProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserStyleProfile)

    @Update
    suspend fun update(profile: UserStyleProfile)

    @Query("DELETE FROM user_style_profile")
    suspend fun deleteAll()
}
