package org.utbot.examples.controlflow

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class CycleDependedConditionTest : UtValueTestCaseChecker(testClass = CycleDependedCondition::class) {
    @Test
    fun testCycleDependedOneCondition() {
        val conditionSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("does not iterate "),
                    DocCodeStmt("for(int i = 0; i < x; i++)"),
                    DocRegularStmt(", "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return 0;"),
                    DocRegularStmt("\n"),
                )
            )
        )
        check(
            CycleDependedCondition::oneCondition,
            eq(3),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..2 && r == 0 },
            { x, r -> x > 2 && r == 1 },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(i == 2): True")),
                keyContain(DocCodeStmt("(i == 2): False")),
                keyMatch(conditionSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testOneCondition_IEquals2"),
                keyMatch("testOneCondition_INotEquals2"),
                keyMatch("testOneCondition_ReturnZero"),
            ),
            summaryDisplayNameChecks = listOf(
                keyContain("i == 2 : True"),
                keyContain("i == 2 : False"),
                keyContain("return 0"),
            )
        )
    }

    @Test
    fun testCycleDependedTwoCondition() {
        val conditionSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("does not iterate "),
                    DocCodeStmt("for(int i = 0; i < x; i++)"),
                    DocRegularStmt(", "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return 0;"),
                    DocRegularStmt("\n"),
                )
            )
        )
        check(
            CycleDependedCondition::twoCondition,
            eq(4),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..3 && r == 0 },
            { x, r -> x == 4 && r == 1 },
            { x, r -> x >= 5 && r == 0 },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(x == 4): False")),
                keyContain(DocCodeStmt("(i > 2): True")),
                keyContain(DocCodeStmt("(i > 2): False")),
                keyMatch(conditionSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testTwoCondition_XNotEquals4"),
                keyMatch("testTwoCondition_XEquals4"),
                keyMatch("testTwoCondition_ILessOrEqual2"),
                keyMatch("testTwoCondition_ReturnZero"),
            ),
            summaryDisplayNameChecks = listOf(
                keyContain("x == 4 : False"),
                keyContain("x == 4 : True"),
                keyContain("i > 2 : False"),
                keyContain("return 0"),
            )
        )
    }


    @Test
    fun testCycleDependedThreeCondition() {
        check(
            CycleDependedCondition::threeCondition,
            eq(4),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..5 && r == 0 },
            { x, r -> x == 6 || x > 8 && r == 1 },
            { x, r -> x == 7 && r == 0 }
        )
    }


    @Test
    fun testCycleDependedOneConditionHigherNumber() {
        check(
            CycleDependedCondition::oneConditionHigherNumber,
            eq(3),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..100 && r == 0 },
            { x, r -> x > 100 && r == 1 && r == 1 }
        )
    }
}