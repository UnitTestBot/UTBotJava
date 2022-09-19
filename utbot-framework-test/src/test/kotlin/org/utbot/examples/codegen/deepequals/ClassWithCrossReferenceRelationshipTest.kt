package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

class ClassWithCrossReferenceRelationshipTest : UtValueTestCaseChecker(
    testClass = ClassWithCrossReferenceRelationship::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testClassWithCrossReferenceRelationship() {
        check(
            ClassWithCrossReferenceRelationship::returnFirstClass,
            eq(2),
            coverage = DoNotCalculate
        )
    }
}