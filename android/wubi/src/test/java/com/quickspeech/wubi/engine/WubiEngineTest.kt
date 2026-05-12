package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiRadicals86
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WubiEngine & WubiRadicals86 测试
 *
 * 注意：WubiEngine 的 companion object 会调用 System.loadLibrary 加载 native 库，
 * 在 JVM 单元测试环境中无法加载。因此 WubiEngine 的 search/reverseLookup 测试
 * 在 instrumented test 中进行。这里只测试 WubiRadicals86 数据类和 WubiScheme 枚举。
 */
class WubiEngineTest {

    // ===== WubiScheme enum tests =====

    @Test
    fun wubiScheme_hasAllValues() {
        val schemes = WubiScheme.values()
        assertTrue(schemes.contains(WubiScheme.WUBI_86))
        assertTrue(schemes.contains(WubiScheme.WUBI_98))
        assertTrue(schemes.contains(WubiScheme.WUBI_NEW))
        assertEquals(3, schemes.size)
    }

    @Test
    fun wubiScheme_values_areDistinct() {
        val schemes = WubiScheme.values()
        assertEquals(schemes.size, schemes.toSet().size)
    }

    // ===== WubiRadicals86 tests =====

    @Test
    fun radicals86_radicalMap_hasAllKeys() {
        val map = WubiRadicals86.radicalMap
        assertTrue("Should have key 'g'", map.containsKey('g'))
        assertTrue("Should have key 'f'", map.containsKey('f'))
        assertTrue("Should have key 'd'", map.containsKey('d'))
        assertTrue("Should have key 's'", map.containsKey('s'))
        assertTrue("Should have key 'a'", map.containsKey('a'))
        assertTrue("Should have key 'h'", map.containsKey('h'))
        assertTrue("Should have key 'j'", map.containsKey('j'))
        assertTrue("Should have key 'k'", map.containsKey('k'))
        assertTrue("Should have key 'l'", map.containsKey('l'))
        assertTrue("Should have key 'm'", map.containsKey('m'))
        assertTrue("Should have key 't'", map.containsKey('t'))
        assertTrue("Should have key 'r'", map.containsKey('r'))
        assertTrue("Should have key 'e'", map.containsKey('e'))
        assertTrue("Should have key 'w'", map.containsKey('w'))
        assertTrue("Should have key 'q'", map.containsKey('q'))
        assertTrue("Should have key 'y'", map.containsKey('y'))
        assertTrue("Should have key 'u'", map.containsKey('u'))
        assertTrue("Should have key 'i'", map.containsKey('i'))
        assertTrue("Should have key 'o'", map.containsKey('o'))
        assertTrue("Should have key 'p'", map.containsKey('p'))
        assertTrue("Should have key 'n'", map.containsKey('n'))
        assertTrue("Should have key 'b'", map.containsKey('b'))
        assertTrue("Should have key 'v'", map.containsKey('v'))
        assertTrue("Should have key 'c'", map.containsKey('c'))
        assertTrue("Should have key 'x'", map.containsKey('x'))
    }

    @Test
    fun radicals86_radicalMap_entriesNotEmpty() {
        val map = WubiRadicals86.radicalMap
        map.forEach { (key, entries) ->
            assertTrue("Radicals for key '$key' should not be empty", entries.isNotEmpty())
        }
    }

    @Test
    fun radicals86_radicalMap_hasKeyRoots() {
        val map = WubiRadicals86.radicalMap
        map.forEach { (key, entries) ->
            assertTrue("Key '$key' should have at least one key root",
                entries.any { it.isKeyRoot })
        }
    }

    @Test
    fun radicals86_level1SimpleCode_hasAllKeys() {
        val simpleCode = WubiRadicals86.level1SimpleCode
        assertEquals(25, simpleCode.size)
        assertFalse(simpleCode.containsKey('z'))
    }

    @Test
    fun radicals86_level1SimpleCode_hasCorrectMappings() {
        val simpleCode = WubiRadicals86.level1SimpleCode
        assertEquals("一", simpleCode['g'])
        assertEquals("地", simpleCode['f'])
        assertEquals("在", simpleCode['d'])
        assertEquals("要", simpleCode['s'])
        assertEquals("工", simpleCode['a'])
        assertEquals("上", simpleCode['h'])
        assertEquals("是", simpleCode['j'])
        assertEquals("中", simpleCode['k'])
        assertEquals("国", simpleCode['l'])
        assertEquals("同", simpleCode['m'])
        assertEquals("和", simpleCode['t'])
        assertEquals("的", simpleCode['r'])
        assertEquals("有", simpleCode['e'])
        assertEquals("人", simpleCode['w'])
        assertEquals("我", simpleCode['q'])
        assertEquals("主", simpleCode['y'])
        assertEquals("产", simpleCode['u'])
        assertEquals("不", simpleCode['i'])
        assertEquals("为", simpleCode['o'])
        assertEquals("这", simpleCode['p'])
        assertEquals("民", simpleCode['n'])
        assertEquals("了", simpleCode['b'])
        assertEquals("发", simpleCode['v'])
        assertEquals("以", simpleCode['c'])
        assertEquals("经", simpleCode['x'])
    }

    @Test
    fun radicals86_level2SimpleCode_hasEntries() {
        val simpleCode = WubiRadicals86.level2SimpleCode
        assertTrue("Level 2 simple code should have entries", simpleCode.isNotEmpty())
    }

    @Test
    fun radicals86_level2SimpleCode_hasExpectedEntries() {
        val simpleCode = WubiRadicals86.level2SimpleCode
        assertEquals("五", simpleCode["gf"])
        assertEquals("天", simpleCode["gd"])
        assertEquals("开", simpleCode["ga"])
    }

    @Test
    fun radicals86_radicalEntry_canBeCreated() {
        val entry = WubiRadicals86.RadicalEntry("王", true)
        assertEquals("王", entry.radical)
        assertTrue(entry.isKeyRoot)

        val entry2 = WubiRadicals86.RadicalEntry("戋", false)
        assertEquals("戋", entry2.radical)
        assertFalse(entry2.isKeyRoot)
    }

    @Test
    fun radicals86_radicalEntry_isDataClass() {
        val e1 = WubiRadicals86.RadicalEntry("王", true)
        val e2 = WubiRadicals86.RadicalEntry("王", true)
        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    @Test
    fun radicals86_level1SimpleCode_allValuesAreSingleChar() {
        val simpleCode = WubiRadicals86.level1SimpleCode
        simpleCode.forEach { (key, value) ->
            assertEquals("Value for key '$key' should be single char", 1, value.length)
        }
    }

    @Test
    fun radicals86_level2SimpleCode_allKeysAreTwoChars() {
        val simpleCode = WubiRadicals86.level2SimpleCode
        simpleCode.forEach { (key, value) ->
            assertEquals("Key '$key' should be 2 chars", 2, key.length)
        }
    }
}
