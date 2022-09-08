package org.utbot.examples.casts

import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class CastClassTest : UtValueTestCaseChecker(
    testClass = CastClass::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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