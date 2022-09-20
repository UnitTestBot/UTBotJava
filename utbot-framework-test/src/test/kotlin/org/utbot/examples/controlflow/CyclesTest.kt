package org.utbot.examples.controlflow

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

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
        val cycleSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("does not iterate "),
                    DocCodeStmt("for(int i = 0; i < x; i++)"),
                    DocRegularStmt(", "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return -1;"),
                    DocRegularStmt("\n"),
                )
            )
        )
        check(
            Cycles::forCycleFour,
            eq(3),
            { x, r -> x <= 0 && r == -1 },
            { x, r -> x in 1..4 && r == -1 },
            { x, r -> x > 4 && r == 1 },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(i > 4): True")),
                keyContain(DocCodeStmt("(i > 4): False")),
                keyMatch(cycleSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testForCycleFour_IGreaterThan4"),
                keyMatch("testForCycleFour_ILessOrEqual4"),
                keyMatch("testForCycleFour_ReturnNegative1")
            ),
            summaryDisplayNameChecks = listOf(
                keyContain("i > 4 : True"),
                keyContain("i > 4 : False"),
                keyContain("return -1")
            )
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
            { x, r -> x >= 42 && r == Cycles().callInnerWhile(x) },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("return innerWhile(value, 42);")),
            ),
            summaryNameChecks = listOf(
                keyMatch("testCallInnerWhile_IterateWhileLoop")
            )
        )
    }

    @Test
    fun testInnerLoop() {
        val innerLoopSummary1 = arrayOf(
            DocRegularStmt("Test "),
            DocRegularStmt("calls "),
            DocRegularStmt("CycleDependedCondition::twoCondition"),
            DocRegularStmt(",\n    there it "),
            DocRegularStmt("iterates the loop "),
            DocRegularStmt(""),
            DocCodeStmt("for(int i = 0; i < x; i++)"),
            DocRegularStmt(" "),
            DocRegularStmt("once"),
            DocRegularStmt(""),
            DocRegularStmt(". "),
            DocRegularStmt("\n    Test "),
//            DocRegularStmt("afterwards "),
            DocRegularStmt("returns from: "),
            DocCodeStmt("return 0;"),
            DocRegularStmt("\n    "),
            DocRegularStmt("\nTest "),
//            DocRegularStmt("afterwards "),
            DocRegularStmt("returns from: "),
            DocCodeStmt("return cycleDependedCondition.twoCondition(value);"),
            DocRegularStmt("\n"),
        )
        val innerLoopSummary2 = arrayOf(
            DocRegularStmt("Test "),
            DocRegularStmt("calls "),
            DocRegularStmt("CycleDependedCondition::twoCondition"),
            DocRegularStmt(",\n    there it "),
            DocRegularStmt("iterates the loop "),
            DocRegularStmt(""),
            DocCodeStmt("for(int i = 0; i < x; i++)"),
            DocRegularStmt(" "),
            DocRegularStmt("4 times"),
            DocRegularStmt(""),
            DocRegularStmt(",\n    "),
            DocRegularStmt("    "),
            DocRegularStmt("inside this loop, the test "),
            DocRegularStmt("executes conditions:\n        "),
            DocRegularStmt(""),
            DocCodeStmt("(i > 2): True"),
            DocRegularStmt(",\n        "),
            DocCodeStmt("(x == 4): True"),
            DocRegularStmt("\n    returns from: "),
            DocCodeStmt("return 1;"),
            DocRegularStmt("\nTest "),
//            DocRegularStmt("afterwards "),
            DocRegularStmt("returns from: "),
            DocCodeStmt("return cycleDependedCondition.twoCondition(value);"),
            DocRegularStmt("\n"),
        )
        val innerLoopSummary3 = arrayOf(
            DocRegularStmt("Test "),
            DocRegularStmt("calls "),
            DocRegularStmt("CycleDependedCondition::twoCondition"),
            DocRegularStmt(",\n    there it "),
            DocRegularStmt("iterates the loop "),
            DocCodeStmt("for(int i = 0; i < x; i++)"),
            DocRegularStmt("5 times"),
            DocRegularStmt("inside this loop, the test "),
            DocRegularStmt("executes conditions:\n        "),
            DocCodeStmt("(x == 4): False"),
            DocRegularStmt("\n    Test "),
            DocRegularStmt("returns from: "),
            DocCodeStmt("return 0;"),
            DocCodeStmt("return cycleDependedCondition.twoCondition(value);"),
        )
        check(
            Cycles::innerLoop,
            ignoreExecutionsNumber,
            { x, r -> x in 1..3 && r == 0 },
            { x, r -> x == 4 && r == 1 },
            { x, r -> x >= 5 && r == 0 },
            // TODO JIRA:1442
/*            summaryTextChecks = listOf(
                keyContain(*innerLoopSummary1),
                keyContain(*innerLoopSummary2),
                keyContain(*innerLoopSummary3)
            ),
            summaryNameChecks = listOf(
                keyMatch("testInnerLoop_ReturnZero"),
                keyMatch("testInnerLoop_XEquals4"),
                keyMatch("testInnerLoop_XNotEquals4")
            )*/
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