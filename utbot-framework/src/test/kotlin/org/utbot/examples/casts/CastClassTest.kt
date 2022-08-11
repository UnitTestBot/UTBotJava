package org.utbot.examples.casts

import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

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