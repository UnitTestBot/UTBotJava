package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class HiddenFieldExampleTest : UtValueTestCaseChecker(testClass = HiddenFieldExample::class) {
    @Test
    // Engine creates HiddenFieldSuccClass instead of HiddenFieldSuperClass, feels wrong field and matchers fail
    fun testCheckHiddenField() {
        check(
            HiddenFieldExample::checkHiddenField,
            eq(4),
            { o, _ -> o == null },
            { o, r -> o != null && o.a != 1 && r == 2 },
            { o, r -> o != null && o.a == 1 && o.b != 2 && r == 2 },
            { o, r -> o != null && o.a == 1 && o.b == 2 && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("SAT-315 Engine cannot work with hidden fields")
    // Engine translates calls to super.b as calls to succ.b
    fun testCheckSuccField() {
        check(
            HiddenFieldExample::checkSuccField,
            eq(5),
            { o, _ -> o == null },
            { o, r -> o.a == 1 && r == 1 },
            { o, r -> o.a != 1 && o.b == 2.0 && r == 2 },
            { o, r -> o.a != 1 && o.b != 2.0 && (o as HiddenFieldSuperClass).b == 3 && r == 3 },
            { o, r -> o.a != 1 && o.b != 2.0 && (o as HiddenFieldSuperClass).b != 3 && r == 4 },
            coverage = DoNotCalculate
        )
    }
}