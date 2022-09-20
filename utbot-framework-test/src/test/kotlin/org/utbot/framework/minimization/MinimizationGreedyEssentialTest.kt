package org.utbot.framework.minimization

import kotlin.math.min
import kotlin.ranges.IntProgression.Companion.fromClosedRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinimizationGreedyEssentialTest {
    @Test
    fun testEmpty() {
        val executions = emptyMap<Int, List<Int>>()
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertTrue(minimizedExecutions.isEmpty())
    }

    @Test
    fun testCustom1() {
        val executions = mapOf(
            1 to listOf(1, 2, 3, 4, 5),
            2 to listOf(2, 3, 4, 5, 6, 7),
            3 to listOf(1, 7),
            4 to listOf(8),
            5 to listOf(1, 8)
        )
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertEquals(listOf(2, 5), minimizedExecutions)
    }

    @Test
    fun testCustom2() {
        val executions = mapOf(
            10 to listOf(1, 2, 3, 4, 5),
            20 to listOf(2, 3, 4, 5, 6, 7),
            21 to listOf(1, 7, 2, 3, 5),
            24 to listOf(8, 5, 6, 7),
            50 to listOf(1, 8)
        )
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertEquals(listOf(20, 50), minimizedExecutions)
    }

    @Test
    fun testBigExecutionAndSmallExecutions() {
        val size = 10000
        val executions = (1..size)
            .associateWith { listOf(it, it + size, it + 2 * size) }
            .toMutableMap().apply {
                put(0, (1..3 * size).toList())
            }
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertEquals(listOf(0), minimizedExecutions.sorted())
    }

    @Test
    fun testSameSizeExecutions() {
        val size = 2000
        val executionSize = 100
        val executions = (0 until size).associateWith { (it until min(size, it + executionSize)).toList() }
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertEquals(fromClosedRange(0, size - 1, executionSize).toList(), minimizedExecutions.sorted())
    }

    @Test
    fun testManyExcluding() {
        val size = 10000
        val executions = (1..size).associateWith { listOf(it, it + size, it + 2 * size) }
        val minimizedExecutions = GreedyEssential.minimize(executions)
        assertEquals((1..size).toList(), minimizedExecutions.sorted())
    }
}