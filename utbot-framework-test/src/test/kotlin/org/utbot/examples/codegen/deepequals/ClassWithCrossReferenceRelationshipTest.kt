package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

class ClassWithCrossReferenceRelationshipTest : UtValueTestCaseChecker(
    testClass = ClassWithCrossReferenceRelationship::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    // TODO: The test is disabled due to [https://github.com/UnitTestBot/UTBotJava/issues/812]
    @Disabled
    @Test
    fun testClassWithCrossReferenceRelationship() {
        check(
            ClassWithCrossReferenceRelationship::returnFirstClass,
            eq(2),
            coverage = DoNotCalculate
        )
    }
}