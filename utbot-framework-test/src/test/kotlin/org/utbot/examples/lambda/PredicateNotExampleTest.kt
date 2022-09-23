package org.utbot.examples.lambda

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

class PredicateNotExampleTest : UtValueTestCaseChecker(testClass = PredicateNotExample::class) {
    @Test
    @Disabled("TODO flaky 0 executions at GitHub runners https://github.com/UnitTestBot/UTBotJava/issues/999")
    fun testPredicateNotExample() {
        check(
            PredicateNotExample::predicateNotExample,
            eq(2),
            { a, r -> a == 5 && r == false },
            { a, r -> a != 5 && r == true },
        )
    }
}
