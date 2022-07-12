package org.utbot.examples.math

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Suppress("SimplifyNegatedBinaryExpression")
internal class DoubleFunctionsTest : UtTestCaseChecker(testClass = DoubleFunctions::class) {
    @Test
    @Tag("slow")
    fun testHypo() {
        check(
            DoubleFunctions::hypo,
            eq(1),
            { a, b, r -> a > 1 && a < 10 && b > 1 && b < 10 && abs(r!! - hypot(a, b)) < 1e-5 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testMax() {
        check(
            DoubleFunctions::max,
            eq(2),
            { a, b, r -> a > b && r == a },
            { a, b, r -> !(a > b) && (r == b || r!!.isNaN()) }
        )
    }

    @Test
    @Tag("slow")
    fun testCircleSquare() {
        checkWithException(
            DoubleFunctions::circleSquare,
            eq(5),
            { radius, r -> radius < 0 && r.isException<IllegalArgumentException>() },
            { radius, r -> radius > 10000 && r.isException<IllegalArgumentException>() },
            { radius, r -> radius.isNaN() && r.isException<IllegalArgumentException>() },
            { radius, r -> Math.PI * radius * radius <= 777.85 && r.getOrNull() == 0.0 },
            { radius, r -> Math.PI * radius * radius > 777.85 && abs(777.85 - r.getOrNull()!!) >= 1e-5 }
        )
    }

    @Test
    @Tag("slow")
    fun testNumberOfRootsInSquareFunction() {
        check(
            DoubleFunctions::numberOfRootsInSquareFunction,
            eq(3), // sometimes solver substitutes a = nan || b = nan || c = nan || some combination of 0 and inf
            { a, b, c, r -> !(b * b - 4 * a * c >= 0) && r == 0 },
            { a, b, c, r -> b * b - 4 * a * c == 0.0 && r == 1 },
            { a, b, c, r -> b * b - 4 * a * c > 0 && r == 2 },
            coverage = DoNotCalculate
        )
    }
}