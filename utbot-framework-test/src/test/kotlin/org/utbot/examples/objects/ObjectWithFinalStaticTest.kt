package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.singleValue

class ObjectWithFinalStaticTest : UtValueTestCaseChecker(
    testClass = ObjectWithFinalStatic::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
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