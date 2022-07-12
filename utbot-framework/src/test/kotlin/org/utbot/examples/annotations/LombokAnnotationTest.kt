package org.utbot.examples.annotations

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.annotations.lombok.EnumWithAnnotations
import org.utbot.examples.annotations.lombok.EnumWithoutAnnotations
import org.utbot.examples.annotations.lombok.NotNullAnnotations
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

/**
 * Tests for code with Lombok annotations
 *
 * We do not calculate coverage here as Lombok always make it pure
 * (see, i.e. https://stackoverflow.com/questions/44584487/improve-lombok-data-code-coverage)
 * and Lombok code is considered to be already tested itself.
 */
class LombokAnnotationTest : UtValueTestCaseChecker(testClass = EnumWithAnnotations::class) {

    @Test
    fun testGetterWithAnnotations() {
        check(
            EnumWithAnnotations::getConstant,
            eq(1),
            coverage = DoNotCalculate,
        )
    }

    @Test
    fun testGetterWithoutAnnotations() {
        check(
            EnumWithoutAnnotations::getConstant,
            eq(1),
            coverage = DoNotCalculate,
        )
    }

    @Test
    fun testNonNullAnnotations() {
        check(
            NotNullAnnotations::lombokNonNull,
            eq(1),
            { value, r -> value == r },
            coverage = DoNotCalculate,
        )
    }
}