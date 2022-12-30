package org.utbot.examples.mixed

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class SerializableExampleTest : UtValueTestCaseChecker(testClass = SerializableExample::class) {

    @Test
    fun testExample() {
        check(
            SerializableExample::example,
            eq(1),
        )
    }
}
