package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class ClassWithCrossReferenceRelationshipTest : UtValueTestCaseChecker(
    testClass = ClassWithCrossReferenceRelationship::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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