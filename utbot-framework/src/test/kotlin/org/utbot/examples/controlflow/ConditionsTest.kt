package org.utbot.examples.controlflow

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.keyContain
import org.utbot.examples.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.junit.jupiter.api.Test

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
