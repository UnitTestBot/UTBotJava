package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq

class ClassWithNullableFieldTest : UtValueTestCaseChecker(
    testClass = ClassWithNullableField::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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