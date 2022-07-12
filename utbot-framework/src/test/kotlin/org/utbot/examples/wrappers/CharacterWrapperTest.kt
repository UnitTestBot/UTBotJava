package org.utbot.examples.wrappers

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// TODO failed Kotlin compilation
internal class CharacterWrapperTest : UtTestCaseChecker(
    testClass = CharacterWrapper::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            CharacterWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x.toInt() >= 100 && r!!.toInt() >= 100 },
            { x, r -> x.toInt() < 100 && r!!.toInt() == x.toInt() + 100 },
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            CharacterWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x.toInt() >= 100 && r!!.toInt() >= 100 },
            { x, r -> x.toInt() < 100 && r!!.toInt() == x.toInt() + 100 },
        )
    }

    @Disabled("Caching char values between -128 and 127 isn't supported JIRA:1481")
    @Test
    fun equalityTest() {
        check(
            CharacterWrapper::equality,
            eq(3),
            { a, b, result -> a == b && a.toInt() <= 127 && result == 1 },
            { a, b, result -> a == b && a.toInt() > 127 && result == 2 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate
        )
    }
}