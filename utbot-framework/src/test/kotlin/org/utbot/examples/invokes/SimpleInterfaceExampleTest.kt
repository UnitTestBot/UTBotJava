package org.utbot.examples.invokes

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class SimpleInterfaceExampleTest : UtValueTestCaseChecker(
    testClass = SimpleInterfaceExample::class
) {
    @Test
    fun testOverrideMethod() {
        check(
            SimpleInterfaceExample::overrideMethod,
            eq(3),
            { o, _, _ -> o == null },
            { o, v, r -> o is SimpleInterfaceImpl && r == v + 2 },
            { o, v, r -> o is Realization && r == v + 5 }
        )
    }

    @Test
    fun testDefaultMethod() {
        check(
            SimpleInterfaceExample::defaultMethod,
            eq(2),
            { o, _, _ -> o == null },
            { o, v, r -> o != null && r == v - 5 }
        )
    }

    @Test
    fun testInvokeMethodFromImplementor() {
        check(
            SimpleInterfaceExample::invokeMethodFromImplementor,
            eq(2),
            { o, _ -> o == null },
            { o, r -> o != null && r == 10 },
        )
    }
}