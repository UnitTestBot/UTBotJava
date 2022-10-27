package org.utbot.examples.annotations.lombok

import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withConcrete

internal class EnumWithoutAnnotationsTest : UtValueTestCaseChecker(testClass = EnumWithoutAnnotations::class) {
    @Test
    fun testGetterWithoutAnnotations() {
        withConcrete(useConcreteExecution = true) { // TODO https://github.com/UnitTestBot/UTBotJava/issues/1249
            check(
                EnumWithoutAnnotations::getConstant,
                eq(1),
                { r -> r == "Constant_1" },
            )
        }
    }
}