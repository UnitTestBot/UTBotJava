package org.utbot.examples.controlflow

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast
import org.utbot.testing.between
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

internal class CyclesTest : UtValueTestCaseChecker(testClass = Cycles::class) {
    @Test
    fun testForCycle() {
        check(
            Cycles::forCycle,
            eq(3),
            { x, r -> x <= 0 && r == -1 },
            { x, r -> x in 1..5 && r == -1 },
            { x, r -> x > 5 && r == 1 }
        )
    }

    @Test
    fun testForCycleFour() {
        check(
            Cycles::forCycleFour,
            eq(3),
            { x, r -> x <= 0 && r == -1 },
            { x, r -> x in 1..4 && r == -1 },
            { x, r -> x > 4 && r == 1 }
        )
    }

    @Test
    fun testForCycleJayHorn() {
        check(
            Cycles::forCycleFromJayHorn,
            eq(2),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x > 0 && r == 2 * x }
        )
    }

    @Test
    fun testFiniteCycle() {
        check(
            Cycles::finiteCycle,
            eq(2),
            { x, r -> x % 519 == 0 && (r as Int) % 519 == 0 },
            { x, r -> x % 519 != 0 && (r as Int) % 519 == 0 }
        )
    }

    @Test
    fun testWhileCycle() {
        check(
            Cycles::whileCycle,
            eq(2),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x > 0 && r == (0 until x).sum() }
        )
    }

    @Test
    fun testCallInnerWhile() {
        check(
            Cycles::callInnerWhile,
            between(1..2),
            { x, r -> x >= 42 && r == Cycles().callInnerWhile(x) }
        )
    }

    @Test
    fun testInnerLoop() {
        check(
            Cycles::innerLoop,
            ignoreExecutionsNumber,
            { x, r -> x in 1..3 && r == 0 },
            { x, r -> x == 4 && r == 1 },
            { x, r -> x >= 5 && r == 0 }
        )
    }

    @Test
    fun testDivideByZeroCheckWithCycles() {
        checkWithException(
            Cycles::divideByZeroCheckWithCycles,
            eq(3),
            { n, _, r -> n < 5 && r.isException<IllegalArgumentException>() },
            { n, x, r -> n >= 5 && x == 0 && r.isException<ArithmeticException>() },
            { n, x, r -> n >= 5 && x != 0 && r.getOrNull() == Cycles().divideByZeroCheckWithCycles(n, x) }
        )
    }

    @Test
    fun moveToExceptionTest() {
        checkWithException(
            Cycles::moveToException,
            eq(3),
            { x, r -> x < 400 && r.isException<IllegalArgumentException>() },
            { x, r -> x > 400 && r.isException<IllegalArgumentException>() },
            { x, r -> x == 400 && r.isException<IllegalArgumentException>() },
            coverage = atLeast(85)
        )
    }
}