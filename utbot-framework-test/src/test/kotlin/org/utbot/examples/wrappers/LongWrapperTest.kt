package org.utbot.examples.wrappers

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutMinimization
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class LongWrapperTest : UtValueTestCaseChecker(
    testClass = LongWrapper::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            LongWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            LongWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Disabled("Caching long values between -128 and 127 doesn't work JIRA:1481")
    @Test
    fun equalityTest() {
        check(
            LongWrapper::equality,
            eq(3),
            { a, b, result -> a == b && a >= -128 && a <= 127 && result == 1 },
            { a, b, result -> a == b && (a < -128 || a > 127) && result == 2 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun parseLong() {
        withoutMinimization { // TODO: JIRA:1506. These branches will be minimized.
            check(
                LongWrapper::parseLong,
                eq(6),
                { line, _ -> line == null },
                { line, _ -> line.isEmpty() },
                { line, _ -> line == "-" },
                { line, _ -> line == "+" },
                { line, _ -> line.startsWith("-") },
                { line, _ -> !line.startsWith("-") },
                coverage = DoNotCalculate
            )
        }
    }
}