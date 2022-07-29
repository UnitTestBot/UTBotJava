package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class HiddenFieldAccessModifiersTest : UtValueTestCaseChecker(testClass = HiddenFieldAccessModifiersExample::class) {
    @Test
    fun testCheckSuperFieldEqualsOne() {
        check(
            HiddenFieldAccessModifiersExample::checkSuperFieldEqualsOne,
            eq(3),
            { o, _ -> o == null },
            { _, r -> r == true },
            { _, r -> r == false},
        )
    }
}