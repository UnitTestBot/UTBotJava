package org.utbot.examples.recursion

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import kotlin.math.pow
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge

internal class RecursionTest : UtValueTestCaseChecker(testClass = Recursion::class) {
    @Test
    fun testFactorial() {
        val factorialSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return 1;"),
                    DocRegularStmt("\n"),
                )
            )
        )

        checkWithException(
            Recursion::factorial,
            eq(3),
            { x, r -> x < 0 && r.isException<IllegalArgumentException>() },
            { x, r -> x == 0 && r.getOrNull() == 1 },
            { x, r -> x > 0 && r.getOrNull() == (1..x).reduce { a, b -> a * b } },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(n < 0): True")),
                keyMatch(factorialSummary),
                keyContain(DocCodeStmt("(n == 0): False"))
            ),
            summaryNameChecks = listOf(
                keyMatch("testFactorial_NLessThanZero"),
                keyMatch("testFactorial_Return1"),
                keyMatch("testFactorial_NNotEqualsZero")
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("-> return 1"),
                keyMatch("n < 0 -> ThrowIllegalArgumentException"),
                keyMatch("-> return 1")
            )
        )
    }

    @Test
    fun testFib() {
        checkWithException(
            Recursion::fib,
            eq(4),
            { x, r -> x < 0 && r.isException<IllegalArgumentException>() },
            { x, r -> x == 0 && r.getOrNull() == 0 },
            { x, r -> x == 1 && r.getOrNull() == 1 },
            { x, r -> x > 1 && r.getOrNull() == Recursion().fib(x) }
        )
    }

    @Test
    @Disabled("Freezes the execution when snd != 0 JIRA:1293")
    fun testSum() {
        check(
            Recursion::sum,
            eq(2),
            { x, y, r -> y == 0 && r == x },
            { x, y, r -> y != 0 && r == x + y }
        )
    }

    @Test
    fun testPow() {
        checkWithException(
            Recursion::pow,
            eq(4),
            { _, y, r -> y < 0 && r.isException<IllegalArgumentException>() },
            { _, y, r -> y == 0 && r.getOrNull() == 1 },
            { x, y, r -> y % 2 == 1 && r.getOrNull() == x.toDouble().pow(y.toDouble()).toInt() },
            { x, y, r -> y % 2 != 1 && r.getOrNull() == x.toDouble().pow(y.toDouble()).toInt() }
        )
    }

    @Test
    fun infiniteRecursionTest() {
        checkWithException(
            Recursion::infiniteRecursion,
            eq(2),
            { x, r -> x > 10000 && r.isException<StackOverflowError>() },
            { x, r -> x <= 10000 && r.isException<StackOverflowError>() },
            coverage = atLeast(50)
        )
    }

    @Test
    fun vertexSumTest() {
        check(
            Recursion::vertexSum,
            between(2..3),
            { x, _ -> x <= 10 },
            { x, _ -> x > 10 }
        )
    }

    @Test
    fun recursionWithExceptionTest() {
        checkWithException(
            Recursion::recursionWithException,
            ge(3),
            { x, r -> x < 42 && r.isException<IllegalArgumentException>() },
            { x, r -> x == 42 && r.isException<IllegalArgumentException>() },
            { x, r -> x > 42 && r.isException<IllegalArgumentException>() },
            coverage = atLeast(50)
        )
    }

    @Test
    fun recursionLoopTest() {
        check(
            Recursion::firstMethod,
            eq(2),
            { x, _ -> x < 4 },
            { x, _ -> x >= 4 },
            coverage = atLeast(50)
        )
    }
}