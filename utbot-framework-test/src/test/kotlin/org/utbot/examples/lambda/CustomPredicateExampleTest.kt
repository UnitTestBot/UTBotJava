package org.utbot.examples.lambda

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class CustomPredicateExampleTest : UtValueTestCaseChecker(testClass = CustomPredicateExample::class) {
    @Test
    fun testNoCapturedValuesPredicateCheck() {
        checkWithException(
            CustomPredicateExample::noCapturedValuesPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedLocalVariablePredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedLocalVariablePredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedParameterPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedParameterPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedStaticFieldPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedStaticFieldPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedNonStaticFieldPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedNonStaticFieldPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }
}
