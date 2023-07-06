package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class ClassWithNullableFieldTest : UtValueTestCaseChecker(
    testClass = ClassWithNullableField::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
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