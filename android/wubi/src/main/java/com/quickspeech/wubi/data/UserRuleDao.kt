package com.quickspeech.wubi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 用户自定义规则 DAO
 */
@Dao
interface UserRuleDao {

    // ==================== 增删改查 ====================

    /** 插入新规则 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: UserRuleEntity): Long

    /** 批量插入规则 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<UserRuleEntity>): List<Long>

    /** 更新规则 */
    @Update
    suspend fun updateRule(rule: UserRuleEntity)

    /** 删除规则 */
    @Delete
    suspend fun deleteRule(rule: UserRuleEntity)

    /** 根据ID删除规则 */
    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    // ==================== 查询 ====================

    /** 获取所有规则（按更新时间降序） */
    @Query("SELECT * FROM user_rules ORDER BY updated_at DESC")
    suspend fun getAllRules(): List<UserRuleEntity>

    /** 获取所有规则（Flow 响应式） */
    @Query("SELECT * FROM user_rules ORDER BY updated_at DESC")
    fun getAllRulesFlow(): Flow<List<UserRuleEntity>>

    /** 只获取启用的规则 */
    @Query("SELECT * FROM user_rules WHERE is_active = 1 ORDER BY usage_count DESC, updated_at DESC")
    suspend fun getActiveRules(): List<UserRuleEntity>

    /** 根据ID查询规则 */
    @Query("SELECT * FROM user_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): UserRuleEntity?

    /** 精确匹配快捷词 */
    @Query("SELECT * FROM user_rules WHERE shortcut = :shortcut AND is_active = 1 LIMIT 1")
    suspend fun findRuleByShortcut(shortcut: String): UserRuleEntity?

    /** 前缀匹配快捷词（用于输入过程中的实时提示） */
    @Query("SELECT * FROM user_rules WHERE shortcut LIKE :prefix || '%' AND is_active = 1 ORDER BY usage_count DESC, shortcut ASC LIMIT :limit")
    suspend fun findRulesByPrefix(prefix: String, limit: Int = 10): List<UserRuleEntity>

    /** 搜索规则（快捷词或描述模糊匹配） */
    @Query("SELECT * FROM user_rules WHERE (shortcut LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR expansion LIKE '%' || :query || '%') AND is_active = 1 ORDER BY usage_count DESC LIMIT :limit")
    suspend fun searchRules(query: String, limit: Int = 20): List<UserRuleEntity>

    /** 按分类查询 */
    @Query("SELECT * FROM user_rules WHERE category = :category AND is_active = 1 ORDER BY usage_count DESC")
    suspend fun getRulesByCategory(category: String): List<UserRuleEntity>

    // ==================== 使用统计 ====================

    /** 增加使用次数 */
    @Query("UPDATE user_rules SET usage_count = usage_count + 1, updated_at = :timestamp WHERE id = :id")
    suspend fun incrementUsageCount(id: Long, timestamp: Long = System.currentTimeMillis())

    // ==================== 数据维护 ====================

    /** 获取规则总数 */
    @Query("SELECT COUNT(*) FROM user_rules")
    suspend fun getRuleCount(): Int

    /** 获取启用规则总数 */
    @Query("SELECT COUNT(*) FROM user_rules WHERE is_active = 1")
    suspend fun getActiveRuleCount(): Int

    /** 删除所有规则 */
    @Query("DELETE FROM user_rules")
    suspend fun deleteAllRules()
}
