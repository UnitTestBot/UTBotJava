package org.utbot.examples.annotations.lombok

import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq

/**
 * Tests for Lombok NonNull annotation
 *
 * We do not calculate coverage here as Lombok always make it pure
 * (see, i.e. https://stackoverflow.com/questions/44584487/improve-lombok-data-code-coverage)
 * and Lombok code is considered to be already tested itself.
 */
internal class NotNullAnnotationsTest : UtValueTestCaseChecker(testClass = NotNullAnnotations::class) {
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