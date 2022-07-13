package org.utbot.examples.codegen

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

class VoidStaticMethodsTest : UtValueTestCaseChecker(testClass = VoidStaticMethodsTestingClass::class) {
    @Test
    fun testInvokeChangeStaticFieldMethod() {
        check(
            VoidStaticMethodsTestingClass::invokeChangeStaticFieldMethod,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInvokeThrowExceptionMethod() {
        check(
            VoidStaticMethodsTestingClass::invokeThrowExceptionMethod,
            eq(3),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInvokeAnotherThrowExceptionMethod() {
        check(
            VoidStaticMethodsTestingClass::invokeAnotherThrowExceptionMethod,
            eq(2),
            coverage = DoNotCalculate
        )
    }
}