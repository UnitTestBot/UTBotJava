package org.utbot.examples.controlflow

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

internal class CycleDependedConditionTest : UtValueTestCaseChecker(testClass = CycleDependedCondition::class) {
    @Test
    fun testCycleDependedOneCondition() {
        check(
            CycleDependedCondition::oneCondition,
            eq(3),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..2 && r == 0 },
            { x, r -> x > 2 && r == 1 }
        )
    }

    @Test
    fun testCycleDependedTwoCondition() {
        check(
            CycleDependedCondition::twoCondition,
            eq(4),
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x in 1..3 && r == 0 },
            { x, r -> x == 4 && r == 1 },
            { x, r -> x >= 5 && r == 0 }
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