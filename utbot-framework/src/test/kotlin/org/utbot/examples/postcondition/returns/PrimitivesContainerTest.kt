package org.utbot.examples.postcondition.returns

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.postcondition.PrimitivesContainer
import org.junit.jupiter.api.Test

class PrimitivesContainerTest : AbstractTestCaseGeneratorTest(PrimitivesContainer::class) {
    @Test
    fun testGetInt() {
        check(
            PrimitivesContainer::getInt,
            branches = ignoreExecutionsNumber,
            { r -> r == 0 }
        )
    }

    @Test
    fun testGetChar() {
        check(
            PrimitivesContainer::getChar,
            branches = ignoreExecutionsNumber,
            { r -> r == 0.toChar() }
        )
    }

    @Test
    fun testGetDouble() {
        check(
            PrimitivesContainer::getDouble,
            branches = ignoreExecutionsNumber,
            { r -> r == 0.0 }
        )
    }

    @Test
    fun testGetFloat() {
        check(
            PrimitivesContainer::getFloat,
            branches = ignoreExecutionsNumber,
            { r -> r == 0.0f }
        )
    }

    @Test
    fun testGetLong() {
        check(
            PrimitivesContainer::getLong,
            branches = ignoreExecutionsNumber,
            { r -> r == 0L }
        )
    }


    @Test
    fun test2() {
        check(
            PrimitivesContainer::getFixedBool,
            branches = ignoreExecutionsNumber,
            { r -> r == false }
        )
    }
}