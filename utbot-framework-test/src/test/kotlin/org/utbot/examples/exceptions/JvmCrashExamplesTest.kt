package org.utbot.examples.exceptions

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutSandbox

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
        withoutSandbox {
            check(
                JvmCrashExamples::crash,
                eq(1), // we expect only one execution after minimization
                // It seems that we can't calculate coverage when the child JVM has crashed
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testCrashPrivileged() {
        check(
            JvmCrashExamples::crashPrivileged,
            eq(1), // we expect only one execution after minimization
            // It seems that we can't calculate coverage when the child JVM has crashed
            coverage = DoNotCalculate
        )
    }
}
