package org.utbot.examples.objects

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.objects.SimpleEnum.FIRST
import org.utbot.examples.objects.SimpleEnum.SECOND
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class EnumsTest : AbstractTestCaseGeneratorTest(testClass = SimpleEnum::class) {
    @Test
    fun testEnumValues() {
        checkStaticMethod(
            SimpleEnum::values,
            eq(1),
            { r -> r.contentEquals(arrayOf(FIRST, SECOND)) },
            // TODO: loader MemoryClassLoader attempted duplicate class definition
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFromCode() {
        checkStaticMethod(
            SimpleEnum::fromCode,
            eq(3),
            { code, r -> code == 1 && r == FIRST },
            { code, r -> code == 2 && r == SECOND },
            { code, r -> code !in 1..2 && r == null }, // IllegalArgumentException
            // TODO: CallerImpl$Method cannot access a member of class SimpleEnum with modifiers "static"
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFromIsFirst() {
        checkStaticMethod(
            SimpleEnum::fromIsFirst,
            eq(2),
            { isFirst, r -> isFirst && r == FIRST },
            { isFirst, r -> !isFirst && r == SECOND },
            // TODO: CallerImpl$Method cannot access a member of class SimpleEnum with modifiers "static"
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("JIRA:1450")
    fun testPublicGetCodeMethod() {
        checkWithThis(
            SimpleEnum::publicGetCode,
            eq(2),
            { enumInstance, r -> enumInstance == FIRST && r == 1 },
            { enumInstance, r -> enumInstance == SECOND && r == 2 },
            coverage = DoNotCalculate
        )
    }
}