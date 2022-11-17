package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class HiddenFieldExampleTest : UtValueTestCaseChecker(testClass = HiddenFieldExample::class) {
    @Test
    fun testCheckHiddenField() {
        check(
            HiddenFieldExample::checkHiddenField,
            eq(4),
            { o, _ -> o == null },
            { o, r -> o != null && o.a != 1 && r == 2 },
            { o, r -> o != null && o.a == 1 && o.b != 2 && r == 2 },
            { o, r -> o != null && o.a == 1 && o.b == 2 && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCheckSuccField() {
        withEnabledTestingCodeGeneration(testCodeGeneration = true) {
            check(
                HiddenFieldExample::checkSuccField,
                eq(5),
                { o, _ -> o == null },
                { o, r -> o.a == 1 && r == 1 },
                { o, r -> o.a != 1 && o.b == 2.0 && r == 2 },
                { o, r -> o.a != 1 && o.b != 2.0 && (o as HiddenFieldSuperClass).b == 3 && r == 3 },
                { o, r -> o.a != 1 && o.b != 2.0 && (o as HiddenFieldSuperClass).b != 3 && r == 4 },
            )
        }
    }
}