package org.utbot.framework.concrete.constructors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListConstructorTest : BaseConstructorTest() {
    @Test
    fun testEmptyList() {
        val arrayList = java.util.LinkedList<Int>()

        val reconstructed = computeReconstructed(arrayList)
        assertEquals(arrayList, reconstructed)
    }

    @Test
    fun testList() {
        val arrayList = java.util.ArrayList<Int>()
        arrayList.addAll(listOf(1, 2, 3, 4))

        val reconstructed = computeReconstructed(arrayList)
        assertEquals(arrayList, reconstructed)
    }

    @Test
    fun testListOfLists() {
        val arrayList = java.util.ArrayList<java.util.ArrayList<Int>?>()
        val arrayList1 = java.util.ArrayList<Int>()
        val arrayList2 = java.util.ArrayList<Int>()
        val arrayList3 = null
        arrayList1.addAll(listOf(1, 2, 3))
        arrayList2.addAll(listOf(10, 20, 30))
        arrayList.addAll(listOf(arrayList1, arrayList2, arrayList3))

        val reconstructed = computeReconstructed(arrayList)
        assertEquals(arrayList, reconstructed)
    }

}