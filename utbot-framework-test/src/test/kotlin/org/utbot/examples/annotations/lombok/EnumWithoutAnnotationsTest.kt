package org.utbot.examples.annotations.lombok

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

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