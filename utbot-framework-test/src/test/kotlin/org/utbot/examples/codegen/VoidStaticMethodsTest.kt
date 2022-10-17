package org.utbot.examples.codegen

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.Compilation

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