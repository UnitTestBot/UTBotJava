package org.utbot.examples.types

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber

internal class GenericsTest : UtValueTestCaseChecker(
    testClass = GenericsTest::class,
    testCodeGeneration = false // TODO empty files are generated https://github.com/UnitTestBot/UTBotJava/issues/1616
) {
    @Test
    fun mapAsParameterTest() {
        check(
            Generics<*>::mapAsParameter,
            eq(2),
            { map, _ -> map == null },
            { map, r -> map != null && r == "value" },
        )
    }

    @Test
    @Disabled("https://github.com/UnitTestBot/UTBotJava/issues/1620 wrong equals")
    fun genericAsFieldTest() {
        check(
            Generics<*>::genericAsField,
            ignoreExecutionsNumber,
            { obj, r -> obj?.field == null && r == false },
            // we can cover this line with any of these two conditions
            { obj, r -> (obj.field != null && obj.field != "abc" && r == false) || (obj.field == "abc" && r == true) },
        )
    }

    @Test
    fun mapAsStaticFieldTest() {
        check(
            Generics<*>::mapAsStaticField,
            ignoreExecutionsNumber,
            { r -> r == "value" },
        )
    }

    @Test
    fun mapAsNonStaticFieldTest() {
        check(
            Generics<*>::mapAsNonStaticField,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, r -> map != null && r == "value" },
        )
    }

    @Test
    fun methodWithRawTypeTest() {
        check(
            Generics<*>::methodWithRawType,
            eq(2),
            { map, _ -> map == null },
            { map, r -> map != null && r == "value" },
        )
    }

    @Test
    fun testMethodWithArrayTypeBoundary() {
        checkWithException(
            Generics<*>::methodWithArrayTypeBoundary,
            eq(1),
            { r -> r.exceptionOrNull() is java.lang.NullPointerException },
            coverage = DoNotCalculate
        )
    }
}