package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

class ClassWithNullableFieldTest : UtValueTestCaseChecker(
    testClass = ClassWithNullableField::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testClassWithNullableFieldInCompound() {
        check(
            ClassWithNullableField::returnCompoundWithNullableField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testClassWithNullableFieldInGreatCompound() {
        check(
            ClassWithNullableField::returnGreatCompoundWithNullableField,
            eq(3),
            coverage = DoNotCalculate
        )
    }
}