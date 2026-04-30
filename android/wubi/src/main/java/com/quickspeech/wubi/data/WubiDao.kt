package com.quickspeech.wubi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 五笔词库 DAO
 */
@Dao
interface WubiDao {

    // ==================== 词库查询 ====================

    /** 精确匹配编码，按词频降序 */
    @Query("SELECT * FROM wubi_words WHERE code = :code ORDER BY frequency DESC")
    suspend fun exactMatch(code: String): List<WubiWordEntry>

    /** 前缀匹配编码，按词频降序（用于输入过程中的实时候选） */
    @Query("SELECT * FROM wubi_words WHERE code LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun prefixMatch(prefix: String, limit: Int = 50): List<WubiWordEntry>

    /** 精确匹配编码，返回Flow（用于Compose响应式更新） */
    @Query("SELECT * FROM wubi_words WHERE code = :code ORDER BY frequency DESC")
    fun exactMatchFlow(code: String): Flow<List<WubiWordEntry>>

    /** 前缀匹配，返回Flow */
    @Query("SELECT * FROM wubi_words WHERE code LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    fun prefixMatchFlow(prefix: String, limit: Int = 50): Flow<List<WubiWordEntry>>

    /** 模糊匹配（容错输入：z键通配）将z替换为_进行SQL LIKE匹配 */
    @Query("SELECT * FROM wubi_words WHERE code LIKE :pattern ORDER BY frequency DESC LIMIT :limit")
    suspend fun fuzzyMatch(pattern: String, limit: Int = 50): List<WubiWordEntry>

    /** 根据汉字查编码（反查） */
    @Query("SELECT * FROM wubi_words WHERE word = :word")
    suspend fun reverseLookup(word: String): List<WubiWordEntry>

    /** 查询词组联想：输入单字后查找包含该字的词组 */
    @Query("SELECT * FROM wubi_words WHERE word LIKE '%' || :char || '%' AND type > 0 ORDER BY frequency DESC LIMIT :limit")
    suspend fun associateWords(char: String, limit: Int = 20): List<WubiWordEntry>

    // ==================== 用户词频 ====================

    /** 获取用户词频 */
    @Query("SELECT * FROM user_frequency WHERE word = :word")
    suspend fun getUserFrequency(word: String): UserFrequencyEntry?

    /** 获取所有用户词频（按次数降序） */
    @Query("SELECT * FROM user_frequency ORDER BY count DESC")
    suspend fun getAllUserFrequencies(): List<UserFrequencyEntry>

    /** 插入用户词频 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFrequency(entry: UserFrequencyEntry)

    /** 更新用户词频 */
    @Update
    suspend fun updateUserFrequency(entry: UserFrequencyEntry)

    /** 增加词频计数 */
    @Query("UPDATE user_frequency SET count = count + 1, last_used = :timestamp WHERE word = :word")
    suspend fun incrementFrequency(word: String, timestamp: Long = System.currentTimeMillis())

    // ==================== 最近使用 ====================

    /** 获取最近使用的词（按时间降序） */
    @Query("SELECT * FROM recent_words ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentWords(limit: Int = 50): List<RecentWordEntry>

    /** 插入或更新最近使用 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentWord(entry: RecentWordEntry)

    /** 清理超过N天的最近使用记录 */
    @Query("DELETE FROM recent_words WHERE timestamp < :expireTime")
    suspend fun cleanOldRecentWords(expireTime: Long)

    // ==================== 数据维护 ====================

    /** 获取词库总数 */
    @Query("SELECT COUNT(*) FROM wubi_words")
    suspend fun getWordCount(): Int

    /** 批量插入词库数据 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WubiWordEntry>)
}
