package org.utbot.examples.controlflow

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ConditionsTest : UtValueTestCaseChecker(testClass = Conditions::class) {
    @Test
    fun testSimpleCondition() {
        val conditionSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("executes conditions:\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(condition): True"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return 1;"),
                    DocRegularStmt("\n"),
                )
            )
        )
        check(
            Conditions::simpleCondition,
            eq(2),
            { condition, r -> !condition && r == 0 },
            { condition, r -> condition && r == 1 },
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(condition): True")),
                keyContain(DocCodeStmt("(condition): False")),
                keyMatch(conditionSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testSimpleCondition_Condition"),
                keyMatch("testSimpleCondition_NotCondition"),
            ),
            summaryDisplayNameChecks = listOf(
                keyContain("condition : True"),
                keyContain("condition : False"),
            )
        )
    }

    @Test
    fun testIfLastStatement() {
        checkWithException(
            Conditions::emptyBranches,
            ignoreExecutionsNumber,
        )
    }
}
