package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

class LocalClassExampleTest : UtValueTestCaseChecker(testClass = LocalClassExample::class) {
    @Test
    fun testLocalClassFieldExample() {
        check(
            LocalClassExample::localClassFieldExample,
            eq(1),
            { y, r -> r == y + 42 }
        )
    }
}
