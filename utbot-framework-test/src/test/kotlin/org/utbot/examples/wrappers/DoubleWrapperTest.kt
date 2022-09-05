package org.utbot.examples.wrappers

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Suppress("SimplifyNegatedBinaryExpression")
internal class DoubleWrapperTest : UtValueTestCaseChecker(testClass = DoubleWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            DoubleWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!!.toDouble() >= 0 },
            { x, r -> (x < 0 || x.isNaN()) && (r!!.toDouble() > 0 || r.isNaN()) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            DoubleWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! >= 0 },
            { x, r -> (x < 0 || x.isNaN()) && (r!! > 0 || r.isNaN()) },
            coverage = DoNotCalculate
        )
    }
}