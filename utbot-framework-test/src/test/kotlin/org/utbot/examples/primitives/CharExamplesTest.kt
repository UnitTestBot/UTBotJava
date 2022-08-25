package org.utbot.examples.primitives

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class CharExamplesTest : UtValueTestCaseChecker(testClass = CharExamples::class) {
    @Test
    fun testCharDiv() {
        checkWithException(
            CharExamples::charDiv,
            eq(2),
            { _, b, r -> b == '\u0000' && r.isException<ArithmeticException>() },
            { a, b, r -> b != '\u0000' && r.getOrNull() == a.toInt() / b.toInt() }
        )
    }

    @Test
    fun testCharNeg() {
        check(
            CharExamples::charNeg,
            eq(2),
            { c, r -> c !in '\u0000'..'\uC350' && r == 1 },
            { c, r -> c in '\u0000'..'\uC350' && r == 2 },
        )
    }

    @Test
    fun testByteToChar() {
        check(
            CharExamples::byteToChar,
            eq(5),
            { b, r -> b == (-1).toByte() && r == -1 },
            { b, r -> b == (-128).toByte() && r == -128 },
            { b, r -> b == 0.toByte() && r == 0 },
            { b, r -> b == 127.toByte() && r == 127 },
            { b, r -> b != (-1).toByte() && b != (-128).toByte() && b != 0.toByte() && b != 127.toByte() && r == 200 },
        )
    }

    @Test
    fun testUpdateObject() {
        checkWithException(
            CharExamples::updateObject,
            eq(3),
            { obj, _, r -> obj == null && r.isException<NullPointerException>() },
            { obj, i, r -> obj != null && i <= 50000 && r.getOrNull()!!.c == '\u0444' },
            { obj, i, r -> obj != null && i.toChar() > 50000.toChar() && r.getOrNull()?.c == i.toChar() },
        )
    }
}