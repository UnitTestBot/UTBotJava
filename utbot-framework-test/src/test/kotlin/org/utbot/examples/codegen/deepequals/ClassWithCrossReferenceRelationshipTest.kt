package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class ClassWithCrossReferenceRelationshipTest : UtValueTestCaseChecker(
    testClass = ClassWithCrossReferenceRelationship::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
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