package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Disabled("Java 11 transition")
internal class ListWrapperReturnsVoidTest : UtValueTestCaseChecker(testClass = ListWrapperReturnsVoidExample::class) {
    @Test
    fun testRunForEach() {
        checkWithException(
            ListWrapperReturnsVoidExample::runForEach,
            eq(4),
            { l, r -> l == null && r.isException<NullPointerException>() },
            { l, r -> l.isEmpty() && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.all { it != null } && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.any { it == null } && r.getOrThrow() > 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testSumPositiveForEach() {
        checkWithException(
            ListWrapperReturnsVoidExample::sumPositiveForEach,
            eq(5),
            { l, r -> l == null && r.isException<NullPointerException>() },
            { l, r -> l.isEmpty() && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.any { it == null } && r.isException<NullPointerException>() },
            { l, r -> l.isNotEmpty() && l.any { it <= 0 } && r.getOrThrow() == l.filter { it > 0 }.sum() },
            { l, r -> l.isNotEmpty() && l.any { it > 0 } && r.getOrThrow() == l.filter { it > 0 }.sum() }
        )
    }
}