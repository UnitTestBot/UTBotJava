package org.utbot.framework.concrete.constructors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapConstructorTest : BaseConstructorTest() {
    @Test
    fun testEmptyLinkedHashMap() {
        val map = java.util.LinkedHashMap<Int, String>()

        val reconstructed = computeReconstructed(map)

        assertEquals(map, reconstructed)
    }

    @Test
    fun testMapOfIntegersToStrings() {
        val map = java.util.LinkedHashMap<Int, String>()
        map.putAll(listOf(1 to "1", 2 to "2", 3 to "3", 1 to "4", 1 to "5", 2 to "6"))

        val reconstructed = computeReconstructed(map)

        assertEquals(map, reconstructed)
    }


    @Test
    fun testMapOfStringsToStrings() {
        val map = java.util.TreeMap<String, String>()
        map.putAll(listOf("1" to "!", "2" to "?", "3" to "#", "3" to "@", "1" to "*"))

        val reconstructed = computeReconstructed(map)

        assertEquals(map, reconstructed)
    }
}