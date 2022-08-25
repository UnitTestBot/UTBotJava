package org.utbot.examples.annotations

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class NotNullAnnotationTest : UtValueTestCaseChecker(testClass = NotNullAnnotation::class) {
    @Test
    fun testDoesNotThrowNPE() {
        check(
            NotNullAnnotation::doesNotThrowNPE,
            eq(1),
            { value, r -> value == r }
        )
    }

    @Test
    fun testThrowsNPE() {
        check(
            NotNullAnnotation::throwsNPE,
            eq(2),
            { value, _ -> value == null },
            { value, r -> value == r }
        )
    }

    @Test
    fun testSeveralParameters() {
        check(
            NotNullAnnotation::severalParameters,
            eq(2),
            { _, second, _, _ -> second == null },
            { first, second, third, result -> first + second + third == result }
        )
    }

    @Test
    fun testUseNotNullableValue() {
        check(
            NotNullAnnotation::useNotNullableValue,
            eq(1),
            { value, r -> value == r }
        )
    }

    @Test
    @Disabled("Annotations for local variables are not supported yet")
    fun testNotNullableVariable() {
        check(
            NotNullAnnotation::notNullableVariable,
            eq(1),
            { first, second, third, r -> first + second + third == r }
        )
    }

    @Test
    fun testNotNullField() {
        check(
            NotNullAnnotation::notNullField,
            eq(1),
            { value, result -> value.boxedInt == result }
        )
    }

    @Test
    fun testNotNullStaticField() {
        checkStatics(
            NotNullAnnotation::notNullStaticField,
            eq(1),
            { statics, result -> result == statics.values.single().value }
        )
    }

    @Test
    fun testJavaxValidationNotNull() {
        check(
            NotNullAnnotation::javaxValidationNotNull,
            eq(1),
            { value, r -> value == r }
        )
    }

    @Test
    fun testFindBugsNotNull() {
        check(
            NotNullAnnotation::findBugsNotNull,
            eq(1),
            { value, r -> value == r }
        )
    }

    @Test
    fun testJavaxNotNull() {
        check(
            NotNullAnnotation::javaxNotNull,
            eq(1),
            { value, r -> value == r }
        )
    }
}