@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.utbot.examples.invokes

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import java.lang.Boolean
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class VirtualInvokeExampleTest : UtValueTestCaseChecker(testClass = VirtualInvokeExample::class) {
    @Test
    fun testSimpleVirtualInvoke() {
        checkWithException(
            VirtualInvokeExample::simpleVirtualInvoke,
            eq(3),
            { v, r -> v < 0 && r.getOrNull() == -2 },
            { v, r -> v == 0 && r.isException<RuntimeException>() },
            { v, r -> v > 0 && r.getOrNull() == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testVirtualNative() {
        check(
            VirtualInvokeExample::virtualNative,
            eq(1),
            { r -> r == Boolean::class.java.modifiers }
        )
    }

    @Test
    fun testGetSigners() {
        check(
            VirtualInvokeExample::virtualNativeArray,
            eq(1),
        )
    }

    @Test
    fun testObjectFromOutside() {
        checkWithException(
            VirtualInvokeExample::objectFromOutside,
            eq(7),
            { o, _, r -> o == null && r.isException<NullPointerException>() },
            { o, v, r -> o != null && o is VirtualInvokeClassSucc && v < 0 && r.getOrNull() == -1 },
            { o, v, r -> o != null && o is VirtualInvokeClassSucc && v == 0 && r.getOrNull() == 0 },
            { o, v, r -> o != null && o is VirtualInvokeClassSucc && v > 0 && r.getOrNull() == 1 },
            { o, v, r -> o != null && o !is VirtualInvokeClassSucc && v < 0 && r.getOrNull() == 2 },
            { o, v, r -> o != null && o !is VirtualInvokeClassSucc && v == 0 && r.isException<RuntimeException>() },
            { o, v, r -> o != null && o !is VirtualInvokeClassSucc && v > 0 && r.getOrNull() == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testDoubleCall() {
        check(
            VirtualInvokeExample::doubleCall,
            eq(2),
            { obj, _ -> obj == null },
            { obj, r -> obj != null && obj.returnX(obj) == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testYetAnotherObjectFromOutside() {
        checkWithException(
            VirtualInvokeExample::yetAnotherObjectFromOutside,
            eq(3),
            { o, r -> o == null && r.isException<NullPointerException>() },
            { o, r -> o != null && o !is VirtualInvokeClassSucc && r.getOrNull() == 1 },
            { o, r -> o != null && o is VirtualInvokeClassSucc && r.getOrNull() == 2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testTwoObjects() {
        checkWithException(
            VirtualInvokeExample::twoObjects,
            eq(3),
            { o, r -> o == null && r.isException<NullPointerException>() },
            { o, r -> o != null && o is VirtualInvokeClassSucc && r.getOrNull() == 1 },
            { o, r -> o != null && o !is VirtualInvokeClassSucc && r.getOrNull() == 2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testNestedVirtualInvoke() {
        checkWithException(
            VirtualInvokeExample::nestedVirtualInvoke,
            eq(3),
            { o, r -> o == null && r.isException<NullPointerException>() },
            { o, r -> o != null && o !is VirtualInvokeClassSucc && r.getOrNull() == 1 },
            { o, r -> o != null && o is VirtualInvokeClassSucc && r.getOrNull() == 2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAbstractClassInstanceFromOutsideWithoutOverrideMethods() {
        check(
            VirtualInvokeExample::abstractClassInstanceFromOutsideWithoutOverrideMethods,
            eq(2),
            { o, _ -> o == null },
            { o, r -> o is VirtualInvokeAbstractClassSucc && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAbstractClassInstanceFromOutside() {
        check(
            VirtualInvokeExample::abstractClassInstanceFromOutside,
            eq(2),
            { o, _ -> o == null },
            { o, r -> o is VirtualInvokeAbstractClassSucc && r == 2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testNullValueInReturnValue() {
        check(
            VirtualInvokeExample::nullValueInReturnValue,
            eq(3),
            { o, _ -> o == null },
            { o, _ -> o is VirtualInvokeClassSucc },
            { o, r -> o is VirtualInvokeClass && r == 10L },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testQuasiImplementationInvoke() {
        check(
            VirtualInvokeExample::quasiImplementationInvoke,
            eq(1),
            { result -> result == 0 },
            coverage = DoNotCalculate
        )
    }
}