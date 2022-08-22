package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.fuzzer.Trie
import org.utbot.fuzzer.stringTrieOf
import org.utbot.fuzzer.trieOf

class TrieTest {

    @Test
    fun simpleTest() {
        val trie = stringTrieOf()
        assertThrows(java.lang.IllegalStateException::class.java) {
            trie.add(emptyList())
        }
        assertEquals(1, trie.add("Tree").count)
        assertEquals(2, trie.add("Tree").count)
        assertEquals(1, trie.add("Trees").count)
        assertEquals(1, trie.add("Treespss").count)
        assertEquals(1, trie.add("Game").count)
        assertEquals(1, trie.add("Gamer").count)
        assertEquals(1, trie.add("Games").count)
        assertEquals(2, trie["Tree"]?.count)
        assertEquals(1, trie["Trees"]?.count)
        assertEquals(1, trie["Gamer"]?.count)
        assertNull(trie["Treesp"])
        assertNull(trie["Treessss"])

        assertEquals(setOf("Tree", "Trees", "Treespss", "Game", "Gamer", "Games"), trie.collect())
    }

    @Test
    fun testSingleElement() {
        val trie = trieOf(listOf(1))
        assertEquals(1, trie.toList().size)
    }

    @Test
    fun testRemoval() {
        val trie = stringTrieOf()
        trie.add("abc")
        assertEquals(1, trie.toList().size)
        trie.add("abcd")
        assertEquals(2, trie.toList().size)
        trie.add("abcd")
        assertEquals(2, trie.toList().size)
        trie.add("abcde")
        assertEquals(3, trie.toList().size)

        assertNotNull(trie.removeCompletely("abcd"))
        assertEquals(2, trie.toList().size)

        assertNull(trie.removeCompletely("ffff"))
        assertEquals(2, trie.toList().size)

        assertNotNull(trie.removeCompletely("abcde"))
        assertEquals(1, trie.toList().size)

        assertNotNull(trie.removeCompletely("abc"))
        assertEquals(0, trie.toList().size)
    }

    @Test
    fun testSearchingAfterDeletion() {
        val trie = stringTrieOf("abc", "abc", "abcde")
        assertEquals(2, trie.toList().size)
        assertEquals(2, trie["abc"]?.count)

        val removed1 = trie.remove("abc")
        assertNotNull(removed1)

        val find = trie["abc"]
        assertNotNull(find)
        assertEquals(1, find!!.count)

        val removed2 = trie.remove("abc")
        assertNotNull(removed2)
    }

    @Test
    fun testTraverse() {
        val trie = Trie(Data::id).apply {
            add((1..10).map { Data(it.toLong(), it) })
            add((1..10).mapIndexed { index, it -> if (index == 5) Data(3L, it) else Data(it.toLong(), it) })
        }

        val paths = trie.toList()
        assertEquals(2, paths.size)
        assertNotEquals(paths[0], paths[1])
    }

    @Test
    fun testNoDuplications() {
        val trie = trieOf(
            (1..10),
            (1..10),
            (1..10),
            (1..10),
            (1..10),
        )

        assertEquals(1, trie.toList().size)
        assertEquals(5, trie[(1..10)]!!.count)
    }

    @Test
    fun testAcceptsNulls() {
        val trie = trieOf(
            listOf(null),
            listOf(null, null),
            listOf(null, null, null),
        )

        assertEquals(3, trie.toList().size)
        for (i in 1 .. 3) {
            assertEquals(1, trie[(1..i).map { null }]!!.count)
        }
    }

    @Test
    fun testAddPrefixAfterWord() {
        val trie = stringTrieOf()
        trie.add("Hello, world!")
        trie.add("Hello")

        assertEquals(setOf("Hello, world!", "Hello"), trie.collect())
    }

    data class Data(val id: Long, val number: Int)
}