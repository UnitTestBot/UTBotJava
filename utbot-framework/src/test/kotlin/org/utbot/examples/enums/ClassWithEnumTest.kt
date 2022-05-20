package org.utbot.examples.enums

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.withoutConcrete
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ClassWithEnumTest : AbstractTestCaseGeneratorTest(testClass = ClassWithEnum::class) {
    @Test
    @Disabled("TODO JIRA:1611")
    fun testOrdinal() {
        withoutConcrete {
            checkAllCombinations(ClassWithEnum::useOrdinal)
        }
    }
}