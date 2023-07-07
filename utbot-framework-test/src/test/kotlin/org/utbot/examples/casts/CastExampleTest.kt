package org.utbot.examples.casts

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException

// TODO failed Kotlin compilation SAT-1332
internal class CastExampleTest : UtValueTestCaseChecker(
    testClass = CastExample::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testSimpleCast() {
        check(
            CastExample::simpleCast,
            eq(3),
            { o, _ -> o != null && o !is CastClassFirstSucc },
            { o, r -> o != null && r is CastClassFirstSucc },
            { o, r -> o == null && r == null },
        )
    }

    @Test
    fun testClassCastException() {
        checkWithException(
            CastExample::castClassException,
            eq(3),
            { o, r -> o == null && r.isException<NullPointerException>() },
            { o, r -> o != null && o !is CastClassFirstSucc && r.isException<ClassCastException>() },
            { o, r -> o != null && o is CastClassFirstSucc && r.isException<ClassCastException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCastUp() {
        check(
            CastExample::castUp,
            eq(1)
        )
    }

    @Test
    fun testCastNullToDifferentTypes() {
        check(
            CastExample::castNullToDifferentTypes,
            eq(1)
        )
    }

    @Test
    fun testFromObjectToPrimitive() {
        check(
            CastExample::fromObjectToPrimitive,
            eq(3),
            { obj, _ -> obj == null },
            { obj, _ -> obj != null && obj !is Int },
            { obj, r -> obj != null && obj is Int && r == obj }
        )
    }

    @Test
    fun testCastFromObjectToInterface() {
        check(
            CastExample::castFromObjectToInterface,
            eq(2),
            { obj, _ -> obj != null && obj !is Colorable },
            { obj, r -> obj != null && obj is Colorable && r == obj },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testComplicatedCast() {
        withEnabledTestingCodeGeneration(testCodeGeneration = false) { // error: package sun.text is not visible
            check(
                CastExample::complicatedCast,
                eq(2),
                { i, a, _ -> i == 0 && a != null && a[i] != null && a[i] !is CastClassFirstSucc },
                { i, a, r -> i == 0 && a != null && a[i] != null && a[i] is CastClassFirstSucc && r is CastClassFirstSucc },
                coverage = DoNotCalculate
            )
        }
    }
}