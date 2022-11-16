package org.utbot.examples.statics.substitution

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutSubstituteStaticsWithSymbolicVariable
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class StaticsSubstitutionTest : UtValueTestCaseChecker(testClass = StaticSubstitutionExamples::class) {

    @Test
    fun lessThanZeroWithSubstitution() {
        check(
            StaticSubstitutionExamples::lessThanZero,
            eq(2),
            { r -> r != 0 },
            { r -> r == 0 },
        )
    }

    @Test
    fun lessThanZeroWithoutSubstitution() {
        withoutSubstituteStaticsWithSymbolicVariable {
            checkWithoutStaticsSubstitution(
                StaticSubstitutionExamples::lessThanZero,
                eq(1),
                { r -> r != 0 },
                coverage = DoNotCalculate,
            )
        }
    }
}