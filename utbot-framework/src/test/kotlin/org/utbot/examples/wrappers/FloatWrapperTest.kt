package org.utbot.examples.wrappers

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

@Suppress("SimplifyNegatedBinaryExpression")
internal class FloatWrapperTest : UtTestCaseChecker(testClass = FloatWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            FloatWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!! >= 0 },
            { x, r -> (x < 0 || x.isNaN()) && (r!! > 0 || r.isNaN()) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            FloatWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! >= 0 },
            { x, r -> (x < 0 || x.isNaN()) && (r!! > 0 || r.isNaN()) },
            coverage = DoNotCalculate
        )
    }
}