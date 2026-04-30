package com.quickspeech.input.ai.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_feedback")
data class UserFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "reply_id") val replyId: String,
    @ColumnInfo(name = "request_id") val requestId: String,
    @ColumnInfo(name = "feedback_type") val feedbackType: String,
    @ColumnInfo(name = "original_text") val originalText: String,
    @ColumnInfo(name = "modified_text") val modifiedText: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "app_category") val appCategory: String
)

@Entity(tableName = "app_category_mappings")
data class AppCategoryMappingEntity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "category") val category: String
)

@Dao
interface UserFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedbackEntity)

    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<UserFeedbackEntity>>

    @Query("SELECT * FROM user_feedback WHERE feedback_type = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getFeedbackByType(type: String, limit: Int = 50): List<UserFeedbackEntity>

    @Query("DELETE FROM user_feedback WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

@Dao
interface AppCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: AppCategoryMappingEntity)

    @Query("SELECT * FROM app_category_mappings WHERE package_name = :packageName")
    suspend fun getCategory(packageName: String): AppCategoryMappingEntity?

    @Query("SELECT * FROM app_category_mappings")
    fun getAllMappings(): Flow<List<AppCategoryMappingEntity>>
}

@Database(
    entities = [UserFeedbackEntity::class, AppCategoryMappingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AiReplyDatabase : RoomDatabase() {
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun appCategoryDao(): AppCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AiReplyDatabase? = null

        fun getInstance(context: Context): AiReplyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiReplyDatabase::class.java,
                    "ai_reply_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
