package org.utbot.examples.controlflow

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber

internal class ConditionsTest : UtValueTestCaseChecker(testClass = Conditions::class) {
    @Test
    fun testSimpleCondition() {
        check(
            Conditions::simpleCondition,
            eq(2),
            { condition, r -> !condition && r == 0 },
            { condition, r -> condition && r == 1 }
        )
    }

    @Test
    fun testIfLastStatement() {
        checkWithException(
            Conditions::emptyBranches,
            ignoreExecutionsNumber,
        )
    }
}
