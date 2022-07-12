package org.utbot.examples.objects

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class RecursiveTypeTest : UtTestCaseChecker(testClass = RecursiveType::class) {
    @Test
    fun testNextValue() {
        check(
            RecursiveType::nextValue,
            eq(5),
            { _, value, _ -> value == 0 },
            { node, _, _ -> node == null },
            { node, _, _ -> node != null && node.next == null },
            { node, value, r -> node?.next != null && node.next.value != value && r == null },
            { node, value, r -> node?.next != null && node.next.value == value && r != null && r.value == value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testWriteObjectFieldTest() {
        check(
            RecursiveType::writeObjectField,
            eq(3),
            { node, _ -> node == null },
            { node, r ->
                node != null && node.next == null && r?.next != null && r.next.value == RecursiveTypeClass().value + 1
            },
            { node, r -> node?.next != null && r?.next != null && node.next.value + 1 == r.next.value },
            coverage = DoNotCalculate
        )
    }
}
