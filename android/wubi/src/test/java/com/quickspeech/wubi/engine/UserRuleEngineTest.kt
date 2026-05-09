package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.data.UserRuleEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRuleEngineTest {

    private lateinit var engine: UserRuleEngine
    private lateinit var mockDao: FakeUserRuleDao

    private class FakeUserRuleDao : UserRuleDao {
        var rules = mutableListOf<UserRuleEntity>()
        var incrementCalledId: Long? = null
        var findShortcutCalled: String? = null
        var findPrefixCalled: String? = null
        var searchCalled: String? = null

        override suspend fun insertRule(rule: UserRuleEntity): Long {
            val id = (rules.maxOfOrNull { it.id } ?: 0) + 1
            rules.add(rule.copy(id = id))
            return id
        }

        override suspend fun insertRules(rules: List<UserRuleEntity>): List<Long> {
            return rules.map { insertRule(it) }
        }

        override suspend fun updateRule(rule: UserRuleEntity) {
            val index = rules.indexOfFirst { it.id == rule.id }
            if (index >= 0) rules[index] = rule
        }

        override suspend fun deleteRule(rule: UserRuleEntity) {
            rules.removeIf { it.id == rule.id }
        }

        override suspend fun deleteRuleById(id: Long) {
            rules.removeIf { it.id == id }
        }

        override suspend fun getAllRules(): List<UserRuleEntity> = rules.toList()

        override fun getAllRulesFlow() = throw UnsupportedOperationException("not needed in test")

        override suspend fun getActiveRules(): List<UserRuleEntity> =
            rules.filter { it.isActive }.sortedByDescending { it.usageCount }

        override suspend fun getRuleById(id: Long): UserRuleEntity? =
            rules.find { it.id == id }

        override suspend fun findRuleByShortcut(shortcut: String): UserRuleEntity? {
            findShortcutCalled = shortcut
            return rules.find { it.shortcut == shortcut && it.isActive }
        }

        override suspend fun findRulesByPrefix(prefix: String, limit: Int): List<UserRuleEntity> {
            findPrefixCalled = prefix
            return rules.filter { it.shortcut.startsWith(prefix) && it.isActive }
                .sortedByDescending { it.usageCount }
                .take(limit)
        }

        override suspend fun searchRules(query: String, limit: Int): List<UserRuleEntity> {
            searchCalled = query
            val q = query.lowercase()
            return rules.filter {
                it.isActive && (
                    it.shortcut.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.expansion.lowercase().contains(q)
                    )
            }.sortedByDescending { it.usageCount }.take(limit)
        }

        override suspend fun getRulesByCategory(category: String): List<UserRuleEntity> =
            rules.filter { it.category == category && it.isActive }
                .sortedByDescending { it.usageCount }

        override suspend fun incrementUsageCount(id: Long, timestamp: Long) {
            incrementCalledId = id
            val index = rules.indexOfFirst { it.id == id }
            if (index >= 0) {
                rules[index] = rules[index].copy(
                    usageCount = rules[index].usageCount + 1,
                    updatedAt = timestamp
                )
            }
        }

        override suspend fun getRuleCount(): Int = rules.size

        override suspend fun getActiveRuleCount(): Int = rules.count { it.isActive }

        override suspend fun deleteAllRules() { rules.clear() }
    }

    @Before
    fun setup() {
        mockDao = FakeUserRuleDao()
        engine = UserRuleEngine(mockDao)
    }

    private fun sampleRule(
        shortcut: String,
        expansion: String,
        description: String = "",
        category: String = "general",
        usageCount: Int = 0,
        isActive: Boolean = true
    ): UserRuleEntity {
        return UserRuleEntity(
            id = 0,
            shortcut = shortcut,
            expansion = expansion,
            description = description,
            category = category,
            usageCount = usageCount,
            isActive = isActive
        )
    }

    @Test
    fun matchRule_exactMatch_returnsRule() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        val result = engine.matchRule("addr")
        assertNotNull(result)
        assertEquals("addr", result!!.shortcut)
        assertEquals("address one", result.expansion)
    }

    @Test
    fun matchRule_noMatch_returnsNull() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        val result = engine.matchRule("xyz")
        assertNull(result)
    }

    @Test
    fun matchRule_blankInput_returnsNull() = runBlocking {
        val result = engine.matchRule("")
        assertNull(result)
    }

    @Test
    fun matchRule_whitespaceInput_returnsNull() = runBlocking {
        val result = engine.matchRule("   ")
        assertNull(result)
    }

    @Test
    fun matchRule_caseInsensitive_returnsRule() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        val result = engine.matchRule("ADDR")
        assertNotNull(result)
        assertEquals("addr", mockDao.findShortcutCalled)
    }

    @Test
    fun matchRule_trimsWhitespace() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        engine.matchRule("  addr  ")
        assertEquals("addr", mockDao.findShortcutCalled)
    }

    @Test
    fun matchRule_inactiveRule_notReturned() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one", isActive = false))
        val result = engine.matchRule("addr")
        assertNull(result)
    }

    @Test
    fun matchRulesPrefix_emptyPrefix_returnsEmpty() = runBlocking {
        val result = engine.matchRulesPrefix("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matchRulesPrefix_matchingPrefix_returnsRules() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        mockDao.insertRule(sampleRule("addr2", "address two"))
        mockDao.insertRule(sampleRule("sig1", "signature one"))
        val result = engine.matchRulesPrefix("addr")
        assertEquals(2, result.size)
    }

    @Test
    fun matchRulesPrefix_noMatch_returnsEmpty() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one"))
        val result = engine.matchRulesPrefix("xyz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matchRulesPrefix_returnsUpToDefaultLimit() = runBlocking {
        for (i in 1..15) {
            mockDao.insertRule(sampleRule("r" + i, "expansion " + i))
        }
        val result = engine.matchRulesPrefix("r")
        assertEquals(10, result.size)
    }

    @Test
    fun matchRulesPrefix_excludesInactive() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one", isActive = true))
        mockDao.insertRule(sampleRule("addr2", "address two", isActive = false))
        val result = engine.matchRulesPrefix("addr")
        assertEquals(1, result.size)
        assertEquals("addr", result[0].shortcut)
    }

    @Test
    fun searchRules_emptyQuery_returnsEmpty() = runBlocking {
        val result = engine.searchRules("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun searchRules_matchingShortcut_returnsRules() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "beijing address"))
        mockDao.insertRule(sampleRule("sig1", "signature"))
        val result = engine.searchRules("addr")
        assertEquals(1, result.size)
        assertEquals("addr", result[0].shortcut)
    }

    @Test
    fun searchRules_matchingDescription_returnsRules() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "beijing address", description = "home address"))
        val result = engine.searchRules("home")
        assertEquals(1, result.size)
    }

    @Test
    fun searchRules_matchingExpansion_returnsRules() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "beijing chaoyang"))
        val result = engine.searchRules("chaoyang")
        assertEquals(1, result.size)
    }

    @Test
    fun searchRules_returnsUpToDefaultLimit() = runBlocking {
        for (i in 1..25) {
            mockDao.insertRule(sampleRule("rule" + i, "expansion " + i))
        }
        val result = engine.searchRules("rule")
        assertEquals(20, result.size)
    }

    @Test
    fun searchRules_excludesInactive() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one", isActive = true))
        mockDao.insertRule(sampleRule("addr2", "address two", isActive = false))
        val result = engine.searchRules("addr")
        assertEquals(1, result.size)
    }

    @Test
    fun recordUsage_incrementsCount() = runBlocking {
        val id = mockDao.insertRule(sampleRule("addr", "beijing address"))
        engine.recordUsage(id)
        assertEquals(id, mockDao.incrementCalledId)
        val rule = mockDao.getRuleById(id)
        assertEquals(1, rule!!.usageCount)
    }

    @Test
    fun getAllActiveRules_returnsOnlyActive() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one", isActive = true))
        mockDao.insertRule(sampleRule("sig1", "signature one", isActive = false))
        mockDao.insertRule(sampleRule("date", "date rule", isActive = true))
        val result = engine.getAllActiveRules()
        assertEquals(2, result.size)
    }

    @Test
    fun getAllActiveRules_sortedByUsageCount() = runBlocking {
        mockDao.insertRule(sampleRule("low", "low freq", usageCount = 1))
        mockDao.insertRule(sampleRule("high", "high freq", usageCount = 100))
        mockDao.insertRule(sampleRule("mid", "mid freq", usageCount = 50))
        val result = engine.getAllActiveRules()
        assertEquals("high", result[0].shortcut)
        assertEquals("mid", result[1].shortcut)
        assertEquals("low", result[2].shortcut)
    }

    @Test
    fun getAllActiveRules_emptyDao_returnsEmpty() = runBlocking {
        val result = engine.getAllActiveRules()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRuleCount_returnsActiveCount() = runBlocking {
        mockDao.insertRule(sampleRule("addr", "address one", isActive = true))
        mockDao.insertRule(sampleRule("sig1", "signature one", isActive = true))
        mockDao.insertRule(sampleRule("date", "date rule", isActive = false))
        val count = engine.getRuleCount()
        assertEquals(2, count)
    }

    @Test
    fun getRuleCount_emptyDao_returnsZero() = runBlocking {
        val count = engine.getRuleCount()
        assertEquals(0, count)
    }
}
