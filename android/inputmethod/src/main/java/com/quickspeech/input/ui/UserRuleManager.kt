package com.quickspeech.input.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.data.UserRuleEntity
import com.quickspeech.wubi.engine.UserRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户规则管理器
 * 提供规则的增删改查、导入导出功能
 */
class UserRuleManager(
    private val context: Context,
    private val ruleDao: UserRuleDao,
    private val ruleEngine: UserRuleEngine
) {
    private val gson = Gson()

    companion object {
        private const val EXPORT_FILENAME_PREFIX = "quickspeech_rules_"
        private const val EXPORT_FILENAME_SUFFIX = ".json"
    }

    // ==================== 规则 CRUD ====================

    /**
     * 添加新规则
     * @return 新规则ID，失败返回 -1
     */
    suspend fun addRule(
        shortcut: String,
        expansion: String,
        category: String = "general",
        description: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val trimmedShortcut = shortcut.lowercase().trim()
        val trimmedExpansion = expansion.trim()

        if (trimmedShortcut.isEmpty() || trimmedExpansion.isEmpty()) return@withContext -1L

        // 检查是否已存在相同快捷词
        val existing = ruleDao.findRuleByShortcut(trimmedShortcut)
        if (existing != null) return@withContext -1L

        val rule = UserRuleEntity(
            shortcut = trimmedShortcut,
            expansion = trimmedExpansion,
            category = category,
            description = description
        )
        ruleDao.insertRule(rule)
    }

    /**
     * 更新现有规则
     */
    suspend fun updateRule(
        ruleId: Long,
        shortcut: String? = null,
        expansion: String? = null,
        category: String? = null,
        description: String? = null,
        isActive: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = ruleDao.getRuleById(ruleId) ?: return@withContext false

        val updated = existing.copy(
            shortcut = shortcut?.lowercase()?.trim() ?: existing.shortcut,
            expansion = expansion?.trim() ?: existing.expansion,
            category = category ?: existing.category,
            description = description ?: existing.description,
            isActive = isActive ?: existing.isActive,
            updatedAt = System.currentTimeMillis()
        )
        ruleDao.updateRule(updated)
        true
    }

    /**
     * 删除规则
     */
    suspend fun deleteRule(ruleId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verify the rule exists before deleting
            val existing = ruleDao.getRuleById(ruleId)
            if (existing == null) return@withContext false
            ruleDao.deleteRuleById(ruleId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取所有规则
     */
    suspend fun getAllRules(): List<UserRuleEntity> = withContext(Dispatchers.IO) {
        ruleDao.getAllRules()
    }

    /**
     * 搜索规则
     */
    suspend fun searchRules(query: String): List<UserRuleEntity> = withContext(Dispatchers.IO) {
        ruleEngine.searchRules(query)
    }

    /**
     * 按分类获取规则
     */
    suspend fun getRulesByCategory(category: String): List<UserRuleEntity> = withContext(Dispatchers.IO) {
        ruleDao.getRulesByCategory(category)
    }

    // ==================== 导入导出 ====================

    /**
     * 导出规则到 JSON 文件
     * @return 导出的文件，失败返回 null
     */
    suspend fun exportRules(): File? = withContext(Dispatchers.IO) {
        try {
            val rules = ruleDao.getAllRules()
            val exportData = mapOf(
                "version" to 1,
                "exportTime" to System.currentTimeMillis(),
                "ruleCount" to rules.size,
                "rules" to rules.map { rule ->
                    mapOf(
                        "shortcut" to rule.shortcut,
                        "expansion" to rule.expansion,
                        "category" to rule.category,
                        "description" to rule.description,
                        "usageCount" to rule.usageCount,
                        "createdAt" to rule.createdAt,
                        "isActive" to rule.isActive
                    )
                }
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "rule_exports").apply { mkdirs() }
            val exportFile = File(exportDir, "$EXPORT_FILENAME_PREFIX$timestamp$EXPORT_FILENAME_SUFFIX")
            exportFile.writeText(gson.toJson(exportData))
            exportFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 JSON 字符串导入规则
     * @return 导入的规则数量，失败返回 -1
     */
    suspend fun importRules(jsonString: String): Int = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(jsonString, type)
            val rulesData = data["rules"] as? List<*> ?: return@withContext -1

            var importedCount = 0
            for (ruleItem in rulesData) {
                val ruleMap = ruleItem as? Map<*, *> ?: continue
                val shortcut = ruleMap["shortcut"] as? String ?: continue
                val expansion = ruleMap["expansion"] as? String ?: continue

                // 跳过已存在的快捷词
                val existing = ruleDao.findRuleByShortcut(shortcut)
                if (existing != null) continue

                val rule = UserRuleEntity(
                    shortcut = shortcut,
                    expansion = expansion,
                    category = ruleMap["category"] as? String ?: "general",
                    description = ruleMap["description"] as? String ?: "",
                    usageCount = (ruleMap["usageCount"] as? Double)?.toInt() ?: 0,
                    createdAt = (ruleMap["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isActive = ruleMap["isActive"] as? Boolean ?: true
                )
                ruleDao.insertRule(rule)
                importedCount++
            }
            importedCount
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 从文件导入规则
     * @return 导入的规则数量，失败返回 -1
     */
    suspend fun importRulesFromFile(file: File): Int = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.readText()
            importRules(jsonString)
        } catch (e: Exception) {
            -1
        }
    }

    // ==================== 预设规则 ====================

    /**
     * 创建默认预设规则（首次使用时）
     */
    suspend fun createDefaultRules(): Int = withContext(Dispatchers.IO) {
        val defaults = listOf(
            UserRuleEntity(
                shortcut = "addr",
                expansion = "",
                category = "address",
                description = "常用地址（请编辑填入实际地址）"
            ),
            UserRuleEntity(
                shortcut = "sig1",
                expansion = "此致敬礼",
                category = "signature",
                description = "邮件签名模板1"
            ),
            UserRuleEntity(
                shortcut = "tel",
                expansion = "",
                category = "general",
                description = "常用电话号码（请编辑填入实际号码）"
            )
        )

        var count = 0
        for (rule in defaults) {
            val existing = ruleDao.findRuleByShortcut(rule.shortcut)
            if (existing == null) {
                ruleDao.insertRule(rule)
                count++
            }
        }
        count
    }
}
