package com.quickspeech.input.ui

import android.content.Context
import com.quickspeech.wubi.data.UserRuleDao
import com.quickspeech.wubi.data.UserRuleEntity
import com.quickspeech.wubi.engine.UserRuleEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

/**
 * Lightweight tests for UserRuleManager import/export logic.
 * Uses Mockito for Context mocking and a fake DAO.
 */
@RunWith(org.mockito.junit.MockitoJUnitRunner::class)
class UserRuleManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCacheDir: java.io.File

    private lateinit var fakeDao: FakeUserRuleDao
    private lateinit var ruleEngine: UserRuleEngine
    private lateinit var manager: UserRuleManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.cacheDir).thenReturn(mockCacheDir)
        `when`(mockCacheDir.mkdirs()).thenReturn(true)

        fakeDao = FakeUserRuleDao()
        ruleEngine = UserRuleEngine(fakeDao)
        manager = UserRuleManager(mockContext, fakeDao, ruleEngine)
    }

    // ==================== addRule tests ====================

    @Test
    fun `addRule succeeds with valid input`() = runBlocking {
        val id = manager.addRule("addr", "123 Main St", "address", "My address")
        assertNotEquals(-1L, id)
    }

    @Test
    fun `addRule returns -1 for empty shortcut`() = runBlocking {
        val id = manager.addRule("", "some text")
        assertEquals(-1L, id)
    }

    @Test
    fun `addRule returns -1 for empty expansion`() = runBlocking {
        val id = manager.addRule("test", "")
        assertEquals(-1L, id)
    }

    @Test
    fun `addRule returns -1 for duplicate shortcut`() = runBlocking {
        manager.addRule("addr", "123 Main St")
        val id = manager.addRule("addr", "456 Oak Ave")
        assertEquals(-1L, id)
    }

    @Test
    fun `addRule normalizes shortcut to lowercase`() = runBlocking {
        val id = manager.addRule("ADDR", "123 Main St")
        assertNotEquals(-1L, id)

        val rules = manager.getAllRules()
        assertEquals(1, rules.size)
        assertEquals("addr", rules[0].shortcut)
    }

    // ==================== updateRule tests ====================

    @Test
    fun `updateRule succeeds for existing rule`() = runBlocking {
        val id = manager.addRule("test", "old text")
        val result = manager.updateRule(id, expansion = "new text")
        assertTrue(result)

        val rules = manager.getAllRules()
        assertEquals("new text", rules[0].expansion)
    }

    @Test
    fun `updateRule returns false for non-existent rule`() = runBlocking {
        val result = manager.updateRule(999L, expansion = "text")
        assertFalse(result)
    }

    // ==================== deleteRule tests ====================

    @Test
    fun `deleteRule removes rule`() = runBlocking {
        val id = manager.addRule("test", "text")
        val result = manager.deleteRule(id)
        assertTrue(result)

        val rules = manager.getAllRules()
        assertEquals(0, rules.size)
    }

    // ==================== searchRules tests ====================

    @Test
    fun `searchRules finds matching rules`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "email", expansion = "user@example.com"))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "addr", expansion = "123 Main St"))

        val result = manager.searchRules("email")
        assertEquals(1, result.size)
        assertEquals("email", result[0].shortcut)
    }

    // ==================== getRulesByCategory tests ====================

    @Test
    fun `getRulesByCategory returns only matching category`() = runBlocking {
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "addr1", expansion = "addr1", category = "address"))
        fakeDao.addRule(UserRuleEntity(id = 2L, shortcut = "addr2", expansion = "addr2", category = "address"))
        fakeDao.addRule(UserRuleEntity(id = 3L, shortcut = "sig1", expansion = "sig1", category = "signature"))

        val result = manager.getRulesByCategory("address")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "address" })
    }

    // ==================== importRules tests ====================

    @Test
    fun `importRules imports valid JSON`() = runBlocking {
        val json = """
        {
            "version": 1,
            "exportTime": 1234567890,
            "ruleCount": 2,
            "rules": [
                {
                    "shortcut": "addr",
                    "expansion": "123 Main St",
                    "category": "address",
                    "description": "Home address",
                    "usageCount": 5,
                    "createdAt": 1234567890,
                    "isActive": true
                },
                {
                    "shortcut": "sig",
                    "expansion": "Best regards",
                    "category": "signature",
                    "description": "Email signature",
                    "usageCount": 10,
                    "createdAt": 1234567890,
                    "isActive": true
                }
            ]
        }
        """.trimIndent()

        val count = manager.importRules(json)
        assertEquals(2, count)

        val rules = manager.getAllRules()
        assertEquals(2, rules.size)
    }

    @Test
    fun `importRules skips duplicate shortcuts`() = runBlocking {
        // Pre-existing rule
        fakeDao.addRule(UserRuleEntity(id = 1L, shortcut = "addr", expansion = "old address"))

        val json = """
        {
            "version": 1,
            "rules": [
                {
                    "shortcut": "addr",
                    "expansion": "new address",
                    "category": "address"
                },
                {
                    "shortcut": "new",
                    "expansion": "new rule",
                    "category": "general"
                }
            ]
        }
        """.trimIndent()

        val count = manager.importRules(json)
        assertEquals(1, count) // Only "new" imported, "addr" skipped

        val rules = manager.getAllRules()
        assertEquals(2, rules.size) // old "addr" + new "new"
    }

    @Test
    fun `importRules returns -1 for invalid JSON`() = runBlocking {
        val count = manager.importRules("not valid json")
        assertEquals(-1, count)
    }

    // ==================== createDefaultRules tests ====================

    @Test
    fun `createDefaultRules creates rules when none exist`() = runBlocking {
        val count = manager.createDefaultRules()
        assertTrue(count > 0)

        val rules = manager.getAllRules()
        assertTrue(rules.any { it.shortcut == "addr" })
        assertTrue(rules.any { it.shortcut == "sig1" })
    }

    @Test
    fun `createDefaultRules does not duplicate existing rules`() = runBlocking {
        manager.createDefaultRules()
        val firstCount = manager.getAllRules().size

        val secondCount = manager.createDefaultRules()
        assertEquals(0, secondCount) // No new rules created

        val finalCount = manager.getAllRules().size
        assertEquals(firstCount, finalCount)
    }
}

/**
 * Reuse the same FakeUserRuleDao from the engine test package.
 * Defined here to avoid cross-package dependencies in tests.
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

    override suspend fun insertRules(rules: List<UserRuleEntity>): List<Long> = rules.map { insertRule(it) }

    override suspend fun updateRule(rule: UserRuleEntity) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) rules[index] = rule
    }

    override suspend fun deleteRule(rule: UserRuleEntity) { rules.removeAll { it.id == rule.id } }

    override suspend fun deleteRuleById(id: Long) { rules.removeAll { it.id == id } }

    override suspend fun getAllRules(): List<UserRuleEntity> = rules.sortedByDescending { it.updatedAt }

    override fun getAllRulesFlow() = throw NotImplementedError()

    override suspend fun getActiveRules(): List<UserRuleEntity> =
        rules.filter { it.isActive }.sortedByDescending { it.usageCount }

    override suspend fun getRuleById(id: Long): UserRuleEntity? = rules.find { it.id == id }

    override suspend fun findRuleByShortcut(shortcut: String): UserRuleEntity? =
        rules.find { it.shortcut == shortcut && it.isActive }

    override suspend fun findRulesByPrefix(prefix: String, limit: Int): List<UserRuleEntity> =
        rules.filter { it.shortcut.startsWith(prefix) && it.isActive }
            .sortedByDescending { it.usageCount }.take(limit)

    override suspend fun searchRules(query: String, limit: Int): List<UserRuleEntity> =
        rules.filter {
            (it.shortcut.contains(query) || it.description.contains(query) || it.expansion.contains(query)) && it.isActive
        }.sortedByDescending { it.usageCount }.take(limit)

    override suspend fun getRulesByCategory(category: String): List<UserRuleEntity> =
        rules.filter { it.category == category && it.isActive }.sortedByDescending { it.usageCount }

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

    fun addRule(rule: UserRuleEntity) { rules.add(rule) }
    fun getRules(): List<UserRuleEntity> = rules.toList()
}
