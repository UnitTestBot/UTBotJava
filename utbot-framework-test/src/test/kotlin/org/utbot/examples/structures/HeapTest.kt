package org.utbot.examples.structures

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.junit.jupiter.api.Test

internal class HeapTest : UtValueTestCaseChecker(testClass = Heap::class) {
    @Test
    fun testIsHeap() {
        val method = Heap::isHeap
        checkStaticMethod(
            method,
            ignoreExecutionsNumber,
            { values, _ -> values == null },
            { values, _ -> values.size < 3 },
            { values, r -> values.size >= 3 && r == method(values) },
            { values, r -> values.size >= 3 && values[1] < values[0] && r == method(values) },
            { values, r -> values.size >= 3 && values[1] >= values[0] && values[2] < values[0] && r == method(values) },
            { values, r -> values.size >= 3 && r == method(values) },
        )
    }
}