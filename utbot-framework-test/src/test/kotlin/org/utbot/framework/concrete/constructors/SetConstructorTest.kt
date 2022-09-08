package org.utbot.framework.concrete.constructors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetConstructorTest : BaseConstructorTest() {
    @Test
    fun testEmptyLinkedHashSet() {
        val set = java.util.LinkedHashSet<Int>()

        val reconstructed = computeReconstructed(set)

        assertEquals(set, reconstructed)
    }

    @Test
    fun testSetOfIntegers() {
        val set = java.util.LinkedHashSet<Int>()
        set.addAll(listOf(1, 2, 3, 1, 1, 2))

        val reconstructed = computeReconstructed(set)

        assertEquals(set, reconstructed)
    }


    @Test
    fun testSetOfStrings() {
        val set = java.util.LinkedHashSet<String>()
        set.addAll(listOf("1", "2", "3", "3", "1"))

        val reconstructed = computeReconstructed(set)

        assertEquals(set, reconstructed)
    }
}