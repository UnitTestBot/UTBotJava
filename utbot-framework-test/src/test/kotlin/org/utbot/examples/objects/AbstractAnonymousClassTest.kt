package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

class AbstractAnonymousClassTest : UtValueTestCaseChecker(testClass = AbstractAnonymousClass::class) {
    @Test
    fun testNonOverriddenMethod() {
        check(
            AbstractAnonymousClass::methodWithoutOverrides,
            eq(1)
        )
    }

    @Test
    fun testOverriddenMethod() {
        // check we have error during execution
        assertThrows<org.opentest4j.AssertionFailedError> {
            check(
                AbstractAnonymousClass::methodWithOverride,
                eq(0)
            )
        }
    }
}