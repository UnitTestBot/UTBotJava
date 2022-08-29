package org.utbot.examples.exceptions

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.withoutSandbox
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

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
