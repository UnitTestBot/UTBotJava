package org.utbot.examples.mixed

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class SimpleNoConditionTest : UtValueTestCaseChecker(testClass = SimpleNoCondition::class) {

    @Test
    fun testNoConditionAdd() {
        check(
            SimpleNoCondition::basicAdd,
            eq(1)
        )
    }

    @Test
    fun testNoConditionPow() {
        check(
            SimpleNoCondition::basicXorInt,
            eq(1)
        )
    }

    @Test
    fun testNoConditionPowBoolean() {
        check(
            SimpleNoCondition::basicXorBoolean,
            eq(1)
        )
    }
}