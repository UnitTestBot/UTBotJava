package org.utbot.examples.mixed

import org.junit.jupiter.api.Disabled
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.ignoreExecutionsNumber

internal class XmlExampleTest : UtValueTestCaseChecker(testClass = XmlExample::class) {
    @Test
    @Disabled("No meaningful branches found https://github.com/UnitTestBot/UTBotJava/issues/943")
    fun testExample() {
        check(
            XmlExample::xmlReader,
            ignoreExecutionsNumber
        )
    }
}