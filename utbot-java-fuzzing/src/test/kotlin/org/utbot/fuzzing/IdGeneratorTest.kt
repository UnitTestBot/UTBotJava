package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator

class IdGeneratorTest {
    private enum class Size { S, M, L, XL }
    private enum class Letter { K, L, M, N }

    @Test
    fun `default id generator returns sequential values`() {
        val idGenerator = ReferencePreservingIntIdGenerator()
        val a = idGenerator.createId()
        val b = idGenerator.createId()
        val c = idGenerator.createId()
        assertTrue(a == ReferencePreservingIntIdGenerator.DEFAULT_LOWER_BOUND + 1)
        assertTrue(b == a + 1)
        assertTrue(c == b + 1)
    }

    @Test
    fun `caching generator returns different ids for different values`() {
        val idGenerator = ReferencePreservingIntIdGenerator()
        val a = idGenerator.getOrCreateIdForValue("a")
        val b = idGenerator.getOrCreateIdForValue("b")
        assertNotEquals(a, b)
    }

    @Test
    fun `caching generator returns same ids for same values`() {
        val idGenerator = ReferencePreservingIntIdGenerator()
        val a = idGenerator.getOrCreateIdForValue("a")
        idGenerator.getOrCreateIdForValue("b")
        val c = idGenerator.getOrCreateIdForValue("a")
        assertEquals(a, c)
    }

    @Test
    fun `caching generator returns consistent ids for enum values`() {
        val idGenerator = ReferencePreservingIntIdGenerator(0)
        val sizeIds = Size.values().map { it to idGenerator.getOrCreateIdForValue(it) }.toMap()

        assertEquals(idGenerator.getOrCreateIdForValue(Size.S), sizeIds[Size.S])
        assertEquals(idGenerator.getOrCreateIdForValue(Size.M), sizeIds[Size.M])
        assertEquals(idGenerator.getOrCreateIdForValue(Size.L), sizeIds[Size.L])
        assertEquals(idGenerator.getOrCreateIdForValue(Size.XL), sizeIds[Size.XL])

        idGenerator.getOrCreateIdForValue(Letter.N)
        idGenerator.getOrCreateIdForValue(Letter.M)
        idGenerator.getOrCreateIdForValue(Letter.L)
        idGenerator.getOrCreateIdForValue(Letter.K)

        assertEquals(1, idGenerator.getOrCreateIdForValue(Size.S))
        assertEquals(2, idGenerator.getOrCreateIdForValue(Size.M))
        assertEquals(3, idGenerator.getOrCreateIdForValue(Size.L))
        assertEquals(4, idGenerator.getOrCreateIdForValue(Size.XL))

        assertEquals(8, idGenerator.getOrCreateIdForValue(Letter.K))
        assertEquals(7, idGenerator.getOrCreateIdForValue(Letter.L))
        assertEquals(6, idGenerator.getOrCreateIdForValue(Letter.M))
        assertEquals(5, idGenerator.getOrCreateIdForValue(Letter.N))
    }

    @Test
    fun `caching generator respects reference equality`() {
        val idGenerator = ReferencePreservingIntIdGenerator()

        val objA = listOf(1, 2, 3)
        val objB = listOf(1, 2, 3)
        val objC = objA

        val idA = idGenerator.getOrCreateIdForValue(objA)
        val idB = idGenerator.getOrCreateIdForValue(objB)
        val idC = idGenerator.getOrCreateIdForValue(objC)

        assertNotEquals(idA, idB)
        assertEquals(idA, idC)
    }

}
