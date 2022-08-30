package org.utbot.examples.controlflow

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import java.math.RoundingMode.CEILING
import java.math.RoundingMode.DOWN
import java.math.RoundingMode.HALF_DOWN
import java.math.RoundingMode.HALF_EVEN
import java.math.RoundingMode.HALF_UP
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutMinimization

internal class SwitchTest : UtValueTestCaseChecker(testClass = Switch::class) {
    @Test
    fun testSimpleSwitch() {
        val switchCaseSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("activates switch case: "),
                    DocCodeStmt("default"),
                    DocRegularStmt(", "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return -1;"),
                    DocRegularStmt("\n"),
                )
            )
        )
        check(
            Switch::simpleSwitch,
            ge(4),
            { x, r -> x == 10 && r == 10 },
            { x, r -> (x == 11 || x == 12) && r == 12 }, // fall-through has it's own branch
            { x, r -> x == 13 && r == 13 },
            { x, r -> x !in 10..13 && r == -1 }, // one for default is enough
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("return 10;")),
                keyContain(DocCodeStmt("return 12;")),
                keyContain(DocCodeStmt("return 12;")),
                keyContain(DocCodeStmt("return 13;")),
                keyMatch(switchCaseSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testSimpleSwitch_Return10"),
                keyMatch("testSimpleSwitch_Return13"),
                keyMatch("testSimpleSwitch_ReturnNegative1"),
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("switch(x) case: 10 -> return 10"),
                keyMatch("switch(x) case: 13 -> return 13"),
                keyMatch("switch(x) case: Default -> return -1"),
            )
        )
    }

    @Test
    fun testLookupSwitch() {
        check(
            Switch::lookupSwitch,
            ge(4),
            { x, r -> x == 0 && r == 0 },
            { x, r -> (x == 10 || x == 20) && r == 20 }, // fall-through has it's own branch
            { x, r -> x == 30 && r == 30 },
            { x, r -> x !in setOf(0, 10, 20, 30) && r == -1 } // one for default is enough
        )
    }

    @Test
    fun testEnumSwitch() {
        withoutMinimization { // TODO: JIRA:1506
            check(
                Switch::enumSwitch,
                eq(7),
                { m, r -> m == null && r == null }, // NPE
                { m, r -> m == HALF_DOWN && r == 1 }, // We will minimize two of these branches
                { m, r -> m == HALF_EVEN && r == 1 }, // We will minimize two of these branches
                { m, r -> m == HALF_UP && r == 1 }, // We will minimize two of these branches
                { m, r -> m == DOWN && r == 2 },
                { m, r -> m == CEILING && r == 3 },
                { m, r -> m !in setOf(HALF_DOWN, HALF_EVEN, HALF_UP, DOWN, CEILING) && r == -1 },
            )
        }
    }
}