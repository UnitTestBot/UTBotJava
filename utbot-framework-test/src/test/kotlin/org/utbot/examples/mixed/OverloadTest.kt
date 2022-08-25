package org.utbot.examples.mixed

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class OverloadTest : UtValueTestCaseChecker(testClass = Overload::class) {
    @Test
    fun testSignOneParam() {
        check(
            Overload::sign,
            eq(3),
            { x, r -> x < 0 && r == -1 },
            { x, r -> x == 0 && r == 0 },
            { x, r -> x > 0 && r == 1 }
        )
    }

    @Test
    fun testSignTwoParams() {
        check(
            Overload::sign,
            eq(3),
            { x, y, r -> x + y < 0 && r == -1 },
            { x, y, r -> x + y == 0 && r == 0 },
            { x, y, r -> x + y > 0 && r == 1 }
        )
    }
}