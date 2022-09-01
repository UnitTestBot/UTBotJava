package org.utbot.examples.annotations.lombok

import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.testcheckers.eq

internal class EnumWithoutAnnotationsTest : UtValueTestCaseChecker(testClass = EnumWithoutAnnotations::class) {
    @Test
    fun testGetterWithoutAnnotations() {
        check(
            EnumWithoutAnnotations::getConstant,
            eq(1),
            { r -> r == "Constant_1" },
        )
    }
}