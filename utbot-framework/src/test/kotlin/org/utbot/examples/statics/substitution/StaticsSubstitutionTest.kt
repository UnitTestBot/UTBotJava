package org.utbot.examples.statics.substitution

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.withoutSubstituteStaticsWithSymbolicVariable
import org.junit.jupiter.api.Test

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