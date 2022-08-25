package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import kotlin.reflect.KFunction2
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

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
