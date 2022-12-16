package org.utbot.examples.codegen

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class VoidStaticMethodsTest : UtValueTestCaseChecker(
    testClass = VoidStaticMethodsTestingClass::class) {
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