package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

class ClassWithNullableFieldTest : UtValueTestCaseChecker(
    testClass = ClassWithNullableField::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testClassWithNullableField() {
        check(
            ClassWithNullableField::returnCompoundWithNullableField,
            eq(2),
            coverage = DoNotCalculate
        )
    }
}