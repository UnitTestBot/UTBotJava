package org.utbot.examples.invokes

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import kotlin.math.ln
import kotlin.math.sqrt
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge

internal class NativeExampleTest : UtValueTestCaseChecker(testClass = NativeExample::class) {
    @Test
    fun testPartialEx() {
        check(
            NativeExample::partialExecution,
            ge(1),
            coverage = atLeast(50)
        )
    }

    @Test
    fun testUnreachableNativeCall() {
        check(
            NativeExample::unreachableNativeCall,
            eq(2),
            { d, r -> !d.isNaN() && r == 1 },
            { d, r -> d.isNaN() && r == 2 },
            coverage = atLeast(50)
        )
    }

    @Test
    @Tag("slow")
    fun testSubstitution() {
        check(
            NativeExample::substitution,
            ignoreExecutionsNumber,
            { x, r -> x > 4 && r == 1 },
            { x, r -> sqrt(x) <= 2 && r == 0 }
        )
    }

    @Test
    fun testUnreachableBranch() {
        check(
            NativeExample::unreachableBranch,
            ge(2),
            { x, r -> x.isNaN() && r == 1 },
            { x, r -> (!ln(x).isNaN() || x < 0) && r == 2 },
            coverage = atLeast(66)
        )
    }
}