package org.utbot.examples.exceptions

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class JvmCrashExamplesTest : UtValueTestCaseChecker(testClass = JvmCrashExamples::class) {
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
