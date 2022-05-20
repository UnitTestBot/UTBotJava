package org.utbot.examples.exceptions

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.primitiveValue
import org.utbot.framework.plugin.api.UtModel
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class JvmCrashExamplesTest : AbstractTestCaseGeneratorTest(testClass = JvmCrashExamples::class) {
    @Test
    @Disabled("JIRA:1527")
    fun testExit() {
        check(
            JvmCrashExamples::exit,
            eq(2)
        )
    }

    @Test
    fun testCrash() {
        check(
            JvmCrashExamples::crash,
            eq(1), // we expect only one execution after minimization
            coverage = DoNotCalculate
        )
    }
}
