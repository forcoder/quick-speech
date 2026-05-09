package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.data.UserRuleEntity

/**
 * 用户自定义规则引擎
 * 负责匹配输入文本与用户定义的快捷规则
 */
class UserRuleEngine(private val ruleDao: UserRuleDao) {

    /**
     * 精确匹配：检查输入是否完全匹配某个规则的快捷词
     * @param input 用户当前输入
     * @return 匹配的规则，无匹配返回 null
     */
    suspend fun matchRule(input: String): UserRuleEntity? {
        if (input.isBlank()) return null
        return ruleDao.findRuleByShortcut(input.lowercase().trim())
    }

    /**
     * 前缀匹配：查找所有以给定前缀开头的规则
     * 用于输入过程中实时提示可用的规则
     * @param prefix 输入前缀
     * @return 匹配的规则列表
     */
    suspend fun matchRulesPrefix(prefix: String): List<UserRuleEntity> {
        if (prefix.isBlank()) return emptyList()
        return ruleDao.findRulesByPrefix(prefix.lowercase().trim())
    }

    /**
     * 搜索规则：模糊匹配快捷词、描述或展开内容
     * @param query 搜索关键词
     * @return 匹配的规则列表
     */
    suspend fun searchRules(query: String): List<UserRuleEntity> {
        if (query.isBlank()) return emptyList()
        return ruleDao.searchRules(query.lowercase().trim())
    }

    /**
     * 记录规则使用（增加使用次数）
     * @param ruleId 规则ID
     */
    suspend fun recordUsage(ruleId: Long) {
        ruleDao.incrementUsageCount(ruleId)
    }

    /**
     * 获取所有启用的规则
     */
    suspend fun getAllActiveRules(): List<UserRuleEntity> {
        return ruleDao.getActiveRules()
    }

    /**
     * 获取规则总数
     */
    suspend fun getRuleCount(): Int {
        return ruleDao.getActiveRuleCount()
    }
}
