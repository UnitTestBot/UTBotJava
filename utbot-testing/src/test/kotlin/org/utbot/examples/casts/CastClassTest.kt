package org.utbot.examples.casts

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class CastClassTest : UtValueTestCaseChecker(
    testClass = CastClass::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
)  {
    @Test
    fun testThisTypeChoice() {
        check(
            CastClass::castToInheritor,
            eq(0),
            coverage = DoNotCalculate
        )
    }
}