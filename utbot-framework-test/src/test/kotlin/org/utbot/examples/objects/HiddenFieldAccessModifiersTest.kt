package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class HiddenFieldAccessModifiersTest : UtValueTestCaseChecker(testClass = HiddenFieldAccessModifiersExample::class) {
    @Test
    fun testCheckSuperFieldEqualsOne() {
        // TODO: currently, codegen can't handle tests with field hiding (see #646)
        withEnabledTestingCodeGeneration(testCodeGeneration = false) {
            check(
                HiddenFieldAccessModifiersExample::checkSuperFieldEqualsOne,
                eq(3),
                { o, _ -> o == null },
                { _, r -> r == true },
                { _, r -> r == false },
            )
        }
    }
}