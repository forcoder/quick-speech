package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.data.UserRuleEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake UserRuleDao for unit testing without Android dependencies
 */
class FakeUserRuleDao : UserRuleDao {
    private val rules = mutableListOf<UserRuleEntity>()
    private var nextId = 1L

    override suspend fun insertRule(rule: UserRuleEntity): Long {
        val toInsert = if (rule.id == 0L) rule.copy(id = nextId++) else rule
        rules.removeAll { it.shortcut == toInsert.shortcut }
        rules.add(toInsert)
        return toInsert.id
    }

    override suspend fun insertRules(rules: List<UserRuleEntity>): List<Long> {
        return rules.map { insertRule(it) }
    }

    override suspend fun updateRule(rule: UserRuleEntity) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) rules[index] = rule
    }

    override suspend fun deleteRule(rule: UserRuleEntity) {
        rules.removeAll { it.id == rule.id }
    }

    override suspend fun deleteRuleById(id: Long) {
        rules.removeAll { it.id == id }
    }

    override suspend fun getAllRules(): List<UserRuleEntity> {
        return rules.sortedByDescending { it.updatedAt }
    }

    override fun getAllRulesFlow() = throw NotImplementedError("Flow not needed in unit tests")

    override suspend fun getActiveRules(): List<UserRuleEntity> {
        return rules.filter { it.isActive }.sortedByDescending { it.usageCount }
    }

    override suspend fun getRuleById(id: Long): UserRuleEntity? {
        return rules.find { it.id == id }
    }

    override suspend fun findRuleByShortcut(shortcut: String): UserRuleEntity? {
        return rules.find { it.shortcut == shortcut && it.isActive }
    }

    override suspend fun findRulesByPrefix(prefix: String, limit: Int): List<UserRuleEntity> {
        return rules.filter { it.shortcut.startsWith(prefix) && it.isActive }
            .sortedByDescending { it.usageCount }
            .take(limit)
    }

    override suspend fun searchRules(query: String, limit: Int): List<UserRuleEntity> {
        return rules.filter {
            (it.shortcut.contains(query) || it.description.contains(query) || it.expansion.contains(query)) && it.isActive
        }.sortedByDescending { it.usageCount }.take(limit)
    }

    override suspend fun getRulesByCategory(category: String): List<UserRuleEntity> {
        return rules.filter { it.category == category && it.isActive }
            .sortedByDescending { it.usageCount }
    }

    override suspend fun incrementUsageCount(id: Long, timestamp: Long) {
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            val rule = rules[index]
            rules[index] = rule.copy(usageCount = rule.usageCount + 1, updatedAt = timestamp)
        }
    }

    override suspend fun getRuleCount(): Int = rules.size

    override suspend fun getActiveRuleCount(): Int = rules.count { it.isActive }

    override suspend fun deleteAllRules() { rules.clear() }

    // Helper methods for testing
    fun addRule(rule: UserRuleEntity) {
        rules.add(rule)
    }

    fun getRules(): List<UserRuleEntity> = rules.toList()
}

class UserRuleEngineTest {

    private lateinit var fakeDao: FakeUserRuleDao
    private lateinit var engine: UserRuleEngine

    @Before
    fun setup() {
        fakeDao = FakeUserRuleDao()
        engine = UserRuleEngine(fakeDao)
    }

    // ==================== matchRule tests ====================

    @Test
    fun `matchRule returns null for blank input`() = runBlocking {
        val result = engine.matchRule("")
        assertNull(result)
    }

    @Test
    fun `matchRule returns null for whitespace input`() = runBlocking {
        val result = engine.matchRule("   ")
        assertNull(result)
    }

    @Test
    fun `matchRule returns null when no rule matches`() = runBlocking {
        val result = engine.matchRule("xyz")
        assertNull(result)
    }

    @Test
    fun `matchRule returns matching rule for exact shortcut`() = runBlocking {
        val rule = UserRuleEntity(
            id = 1L,
            shortcut = "addr",
            expansion = "123 Main St, City, Country",
            category = "address"
        )
        fakeDao.addRule(rule)

        val result = engine.matchRule("addr")

        assertEquals(rule.shortcut, result?.shortcut)
        assertEquals("123 Main St, City, Country", result?.expansion)
    }

    @Test
    fun `matchRule converts input to lowercase`() = runBlocking {
        val rule = UserRuleEntity(
            id = 1L,
            shortcut = "addr",
            expansion = "123 Main St",
            category = "address"
        )
        fakeDao.addRule(rule)

        val result = engine.matchRule("ADDR")

        assertEquals("addr", result?.shortcut)
    }

    @Test
    fun `matchRule trims input`() = runBlocking {
        val rule = UserRuleEntity(
            id = 1L,
            shortcut = "sig",
            expansion = "Best regards",
            category = "signature"
        )
        fakeDao.addRule(rule)

        val result = engine.matchRule("  sig  ")

        assertEquals("sig", result?.shortcut)
    }

    @Test
    fun `matchRule does not match inactive rules`() = runBlocking {
        val rule = UserRuleEntity(
            id = 1L,
            shortcut = "old",
            expansion = "old text",
            isActive = false
        )
        fakeDao.addRule(rule)

        val result = engine.matchRule("old")

        assertNull(result)
    }

    // ==================== matchRulesPrefix tests ====================

    @Test
    fun `matchRulesPrefix returns empty list for blank input`() = runBlocking {
        val result = engine.matchRulesPrefix("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchRulesPrefix returns matching rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "addr", expansion = "123 Main St", category = "address"))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "address", expansion = "456 Oak Ave", category = "address"))
        fakeDao.addRule(UserRuleEntity(id = 3L, shortcut = "sig", expansion = "Best regards", category = "signature"))

        val result = engine.matchRulesPrefix("addr")

        assertEquals(2, result.size)
        assertTrue(result.all { it.shortcut.startsWith("addr") })
    }

    @Test
    fun `matchRulesPrefix returns empty list when no prefix matches`() = runBlocking {
        val result = engine.matchRulesPrefix("zzz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchRulesPrefix converts input to lowercase`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "sig", expansion = "Best regards"))

        val result = engine.matchRulesPrefix("SIG")

        assertEquals(1, result.size)
        assertEquals("sig", result[0].shortcut)
    }

    @Test
    fun `matchRulesPrefix does not include inactive rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "addr", expansion = "active", isActive = true))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "address", expansion = "inactive", isActive = false))

        val result = engine.matchRulesPrefix("addr")

        assertEquals(1, result.size)
        assertEquals("addr", result[0].shortcut)
    }

    // ==================== searchRules tests ====================

    @Test
    fun `searchRules returns empty list for blank input`() = runBlocking {
        val result = engine.searchRules("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchRules returns matching rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "email", expansion = "user@example.com", category = "general"))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "addr", expansion = "123 Main St", category = "address"))

        val result = engine.searchRules("email")

        assertEquals(1, result.size)
        assertEquals("email", result[0].shortcut)
    }

    @Test
    fun `searchRules matches description`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "x", expansion = "text", description = "my email address"))

        val result = engine.searchRules("email")

        assertEquals(1, result.size)
    }

    // ==================== recordUsage tests ====================

    @Test
    fun `recordUsage increments usage count`() = runBlocking {
        val rule = UserRuleEntity(id = 1L, shortcut = "test", expansion = "test text", usageCount = 5)
        fakeDao.addRule(rule)

        engine.recordUsage(1L)

        val updated = fakeDao.getRuleById(1L)
        assertEquals(6, updated?.usageCount)
    }

    // ==================== getAllActiveRules tests ====================

    @Test
    fun `getAllActiveRules returns only active rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "a", expansion = "aaa", isActive = true))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "b", expansion = "bbb", isActive = false))
        fakeDao.addRule(UserRuleEntity(id = 3L, shortcut = "c", expansion = "ccc", isActive = true))

        val result = engine.getAllActiveRules()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isActive })
    }

    // ==================== getRuleCount tests ====================

    @Test
    fun `getRuleCount returns count of active rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "a", expansion = "aaa", isActive = true))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "b", expansion = "bbb", isActive = false))
        fakeDao.addRule(UserRuleEntity(id = 3L, shortcut = "c", expansion = "ccc", isActive = true))

        val count = engine.getRuleCount()

        assertEquals(2, count)
    }

    @Test
    fun `getRuleCount returns zero when no active rules`() = runBlocking {
        val count = engine.getRuleCount()
        assertEquals(0, count)
    }

    // ==================== Integration-like tests ====================

    @Test
    fun `full workflow - add rule, match, record usage`() = runBlocking {
        // Add rules
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "addr", expansion = "123 Main St", category = "address"))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "sig", expansion = "Best regards, John", category = "signature"))

        // Match
        val match = engine.matchRule("addr")
        assertEquals("addr", match?.shortcut)
        assertEquals("123 Main St", match?.expansion)

        // Record usage
        engine.recordUsage(match!!.id)

        // Verify usage count incremented
        val updated = fakeDao.getRuleById(match.id)
        assertEquals(1, updated?.usageCount)
    }

    @Test
    fun `prefix match returns rules sorted by usage count`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "ab", expansion = "a", usageCount = 10))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "abc", expansion = "b", usageCount = 50))
        fakeDao.addRule(UserRuleEntity(id = 3L, shortcut = "abcd", expansion = "c", usageCount = 30))

        val result = engine.matchRulesPrefix("ab")

        assertEquals(3, result.size)
        assertEquals("abc", result[0].shortcut) // highest usage first
        assertEquals("abcd", result[1].shortcut)
        assertEquals("ab", result[2].shortcut)
    }
}
