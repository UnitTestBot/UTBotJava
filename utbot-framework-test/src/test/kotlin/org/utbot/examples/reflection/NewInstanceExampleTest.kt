package org.utbot.examples.reflection

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

class NewInstanceExampleTest : UtValueTestCaseChecker(NewInstanceExample::class) {
    @Test
    fun testNewInstanceExample() {
        check(
            NewInstanceExample::createWithReflectionExample,
            eq(1),
            { r -> r == 0 }
        )
    }
}
