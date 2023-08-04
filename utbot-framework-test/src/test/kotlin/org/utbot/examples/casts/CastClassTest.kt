package org.utbot.examples.casts

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class CastClassTest : UtValueTestCaseChecker(
    testClass = CastClass::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
)  {
    @Test
    fun testThisTypeChoice() {
        check(
            CastClass::castToInheritor,
            eq(0),
            coverage = DoNotCalculate
        )
    }
}