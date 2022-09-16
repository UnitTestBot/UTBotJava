package org.utbot.examples.annotations.lombok

import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.testcheckers.eq

/**
 * Tests for Lombok annotations
 *
 * We do not calculate coverage here as Lombok always make it pure
 * (see, i.e. https://stackoverflow.com/questions/44584487/improve-lombok-data-code-coverage)
 * and Lombok code is considered to be already tested itself.
 */
internal class EnumWithAnnotationsTest : UtValueTestCaseChecker(testClass = EnumWithAnnotations::class) {
    @Test
    fun testGetterWithAnnotations() {
        check(
            EnumWithAnnotations::getConstant,
            eq(1),
            { r -> r == "Constant_1" },
            coverage = DoNotCalculate,
        )
    }
}