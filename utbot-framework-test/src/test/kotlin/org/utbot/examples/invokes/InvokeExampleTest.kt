package org.utbot.examples.invokes

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class InvokeExampleTest : UtValueTestCaseChecker(testClass = InvokeExample::class) {
    @Test
    fun testSimpleFormula() {
        check(
            InvokeExample::simpleFormula,
            eq(3),
            { fst, _, _ -> fst < 100 },
            { _, snd, _ -> snd < 100 },
            { fst, snd, r -> fst >= 100 && snd >= 100 && r == (fst + 5) * (snd / 2) },
        )
    }

    @Test
    fun testChangeObjectValueByMethod() {
        check(
            InvokeExample::changeObjectValueByMethod,
            eq(2),
            { o, _ -> o == null },
            { o, r -> o != null && r?.value == 4 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testParticularValue() {
        check(
            InvokeExample::particularValue,
            eq(3),
            { o, _ -> o == null },
            { o, _ -> o != null && o.value < 0 },
            { o, r -> o != null && o.value >= 0 && r?.value == 12 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateObjectFromValue() {
        check(
            InvokeExample::createObjectFromValue,
            eq(2),
            { value, r -> value == 0 && r?.value == 1 },
            { value, r -> value != 0 && r?.value == value }
        )
    }

    @Test
    fun testGetNullOrValue() {
        check(
            InvokeExample::getNullOrValue,
            eq(3),
            { o, _ -> o == null },
            { o, r -> o != null && o.value < 100 && r == null },
            { o, r -> o != null && o.value >= 100 && r?.value == 5 },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testConstraintsFromOutside() {
        check(
            InvokeExample::constraintsFromOutside,
            eq(3),
            { value, r -> value >= 0 && r == value },
            { value, r -> value == Int.MIN_VALUE && r == 0 },
            { value, r -> value < 0 && value != Int.MIN_VALUE && r == -value },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testConstraintsFromInside() {
        check(
            InvokeExample::constraintsFromInside,
            eq(3),
            { value, r -> value >= 0 && r == 1 },
            { value, r -> value == Int.MIN_VALUE && r == 1 },
            { value, r -> value < 0 && value != Int.MIN_VALUE && r == 1 },
        )
    }

    @Test
    fun testAlwaysNPE() {
        checkWithException(
            InvokeExample::alwaysNPE,
            eq(4),
            { o, r -> o == null && r.isException<NullPointerException>() },
            { o, r -> o != null && o.value == 0 && r.isException<NullPointerException>() },
            { o, r -> o != null && o.value < 0 && r.isException<NullPointerException>() },
            { o, r -> o != null && o.value > 0 && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testExceptionInNestedMethod() {
        checkWithException(
            InvokeExample::exceptionInNestedMethod,
            eq(3),
            { o, _, r -> o == null && r.isException<NullPointerException>() },
            { o, value, r -> o != null && value < 0 && r.isException<IllegalArgumentException>() },
            { o, value, r -> o != null && value >= 0 && value == (r.getOrNull() as InvokeClass).value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFewNestedExceptions() {
        checkWithException(
            InvokeExample::fewNestedException,
            eq(5),
            { o, _, r -> o == null && r.isException<NullPointerException>() },
            { o, value, r -> o != null && value < 10 && r.isException<IllegalArgumentException>() },
            { o, value, r -> o != null && value in 10..99 && r.isException<IllegalArgumentException>() },
            { o, value, r -> o != null && value in 100..9999 && r.isException<IllegalArgumentException>() },
            { o, value, r -> o != null && value >= 10000 && value == (r.getOrNull() as InvokeClass).value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testDivBy() {
        checkWithException(
            InvokeExample::divBy,
            eq(4),
            { o, _, r -> o == null && r.isException<NullPointerException>() },
            { o, _, r -> o != null && o.value < 1000 && r.isException<IllegalArgumentException>() },
            { o, den, r -> o != null && o.value >= 1000 && den == 0 && r.isException<ArithmeticException>() },
            { o, den, r -> o != null && o.value >= 1000 && den != 0 && r.getOrNull() == o.value / den },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUpdateValue() {
        check(
            InvokeExample::updateValue,
            eq(4),
            { o, _, _ -> o == null },
            { o, _, r -> o != null && o.value > 0 && r != null && r.value > 0 },
            { o, value, r -> o != null && o.value <= 0 && value > 0 && r?.value == value },
            { o, value, _ -> o != null && o.value <= 0 && value <= 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testNullAsParameter() {
        check(
            InvokeExample::nullAsParameter,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testChangeArrayWithAssignFromMethod() {
        check(
            InvokeExample::changeArrayWithAssignFromMethod,
            eq(3),
            { a, _ -> a == null },
            { a, r -> a != null && a.isEmpty() && r != null && r.isEmpty() },
            { a, r ->
                require(a != null && r != null)
                a.isNotEmpty() && r.size == a.size && a.map { it + 5 } == r.toList() && !a.contentEquals(r)
            }
        )
    }

    @Test
    fun testChangeArrayByMethod() {
        check(
            InvokeExample::changeArrayByMethod,
            ignoreExecutionsNumber,
            { a, _ -> a == null },
            { a, r -> a != null && a.isNotEmpty() && r != null && r.size == a.size && a.map { it + 5 } == r.toList() }
        )
    }

    @Test
    fun testArrayCopyExample() {
        check(
            InvokeExample::arrayCopyExample,
            eq(5),
            { a, _ -> a == null },
            { a, _ -> a != null && a.size < 3 },
            { a, r -> a != null && a.size >= 3 && a[0] <= a[1] && r == null },
            { a, r -> a != null && a.size >= 3 && a[0] > a[1] && a[1] <= a[2] && r == null },
            { a, r -> a != null && a.size >= 3 && a[0] > a[1] && a[1] > a[2] && r.contentEquals(a) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUpdateValues() {
        check(
            InvokeExample::updateValues,
            eq(4),
            { fst, _, _ -> fst == null },
            { fst, snd, _ -> fst != null && snd == null },
            { fst, snd, r -> fst != null && snd != null && fst !== snd && r == 1 },
            { fst, snd, _ -> fst != null && snd != null && fst === snd },
            coverage = DoNotCalculate
        )
    }
}