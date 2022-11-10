package org.utbot.examples.objects

import kotlin.reflect.KFunction2
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class ObjectWithThrowableConstructorTest : UtValueTestCaseChecker(testClass = ObjectWithThrowableConstructor::class) {
    @Test
    @Disabled("SAT-1500 Support verification of UtAssembleModel for possible exceptions")
    fun testThrowableConstructor() {
        val method: KFunction2<Int, Int, ObjectWithThrowableConstructor> = ::ObjectWithThrowableConstructor
        checkStaticMethod(
            method,
            eq(2),
            // TODO: SAT-933 Add support for constructor testing
            coverage = DoNotCalculate
        )
    }
}
