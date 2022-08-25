package org.utbot.examples.thirdparty.numbers

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

// example from Apache common-numbers
internal class ArithmeticUtilsTest : UtValueTestCaseChecker(testClass = ArithmeticUtils::class) {
    @Test
    @Tag("slow")
    fun testPow() {
        check(
            ArithmeticUtils::pow,
            eq(11),
            { _, e, _ -> e < 0 }, // IllegalArgumentException
            { k, e, r -> k == 0 && e == 0 && r == 1 },
            { k, e, r -> k == 0 && e != 0 && r == 0 },
            { k, _, r -> k == 1 && r == 1 },
            { k, e, r -> k == -1 && e and 1 == 0 && r == 1 },
            { k, e, r -> k == -1 && e and 1 != 0 && r == -1 },
            { _, e, _ -> e >= 31 }, // ArithmeticException
            { k, e, r -> k !in -1..1 && e in 0..30 && r == pow(k, e) },

            // And 2 additional branches here with ArithmeticException reg integer overflow
            { k, e, r -> k !in -1..1 && e in 0..30 && r == null },
        )
    }
}

fun pow(k: Int, e: Int): Int {
    var exp = e
    var result = 1
    var k2p = k
    while (true) {
        if (exp and 0x1 != 0) {
            result = Math.multiplyExact(result, k2p)
        }
        exp = exp shr 1
        if (exp == 0) {
            break
        }
        k2p = Math.multiplyExact(k2p, k2p)
    }
    return result
}