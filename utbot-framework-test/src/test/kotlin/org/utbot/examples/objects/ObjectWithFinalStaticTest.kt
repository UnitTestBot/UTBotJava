package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.singleValue
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

class ObjectWithFinalStaticTest : UtValueTestCaseChecker(
    testClass = ObjectWithFinalStatic::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testParameterEqualsFinalStatic() {
        checkStatics(
            ObjectWithFinalStatic::parameterEqualsFinalStatic,
            eq(2),
            { key, _, statics, result -> key != statics.singleValue() as Int  && result == -420 },
            // matcher checks equality by value, but branch is executed if objects are equal by reference
            { key, i, statics, result -> key == statics.singleValue() && i == result },
            coverage = DoNotCalculate
        )
    }
}