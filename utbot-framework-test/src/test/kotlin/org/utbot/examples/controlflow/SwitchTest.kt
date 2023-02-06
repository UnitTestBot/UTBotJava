package org.utbot.examples.controlflow

import java.math.RoundingMode.CEILING
import java.math.RoundingMode.DOWN
import java.math.RoundingMode.HALF_DOWN
import java.math.RoundingMode.HALF_EVEN
import java.math.RoundingMode.HALF_UP
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutMinimization
import org.utbot.testing.UtValueTestCaseChecker

internal class SwitchTest : UtValueTestCaseChecker(testClass = Switch::class) {
    @Test
    fun testSimpleSwitch() {
        check(
            Switch::simpleSwitch,
            ge(4),
            { x, r -> x == 10 && r == 10 },
            { x, r -> (x == 11 || x == 12) && r == 12 }, // fall-through has it's own branch
            { x, r -> x == 13 && r == 13 },
            { x, r -> x !in 10..13 && r == -1 }, // one for default is enough
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