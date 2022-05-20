package org.utbot.analytics.guava.math

import org.utbot.analytics.examples.SummaryTestCaseGeneratorTest
import guava.examples.math.IntMath
import org.junit.jupiter.api.Test

class SummaryIntMath : SummaryTestCaseGeneratorTest(
    IntMath::class,
) {

    //TODO SAT-1205
    @Test
    fun testLog2() {
        checkOneArgument(
            IntMath::log2,
            summaryKeys = listOf()
        )
    }

    @Test
    fun testPow() {
        val summaryPow1 = "<pre>\n" +
                "Test activates switch case: {@code 2}, returns from: {@code return 1;}\n" +
                "</pre>"
        val summaryPow2 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code (k < Integer.SIZE): False}\n" +
                "returns from: {@code return 0;}\n" +
                "</pre>"
        val summaryPow3 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code ((k < Integer.SIZE)): False}\n" +
                "returns from: {@code return (k < Integer.SIZE) ? (1 << k) : 0;}\n" +
                "</pre>"
        val summaryPow4 = "<pre>\n" +
                "Test iterates the loop {@code for(int accum = 1; ; k >>= 1)} once,\n" +
                "    inside this loop, the test returns from: {@code return b * accum;}\n" +
                "</pre>"
        val summaryPow5 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code ((k < Integer.SIZE)): True}\n" +
                "returns from: {@code return (k < Integer.SIZE) ? (1 << k) : 0;}\n" +
                "</pre>"
        val summaryPow6 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code ((k == 0)): False}\n" +
                "returns from: {@code return (k == 0) ? 1 : 0;}\n" +
                "</pre>"
        val summaryPow7 = "<pre>\n" +
                "Test iterates the loop {@code for(int accum = 1; ; k >>= 1)} once,\n" +
                "    inside this loop, the test returns from: {@code return accum;}\n" +
                "</pre>"
        val summaryPow8 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code ((k == 0)): True}\n" +
                "returns from: {@code return (k == 0) ? 1 : 0;}\n" +
                "</pre>"
        val summaryPow9 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code (k < Integer.SIZE): True},\n" +
                "    {@code (((k & 1) == 0)): True}\n" +
                "returns from: {@code return ((k & 1) == 0) ? (1 << k) : -(1 << k);}\n" +
                "</pre>"
        val summaryPow10 = "<pre>\n" +
                "Test iterates the loop {@code for(int accum = 1; ; k >>= 1)} twice,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    {@code (((k & 1) == 0)): False}\n" +
                ", returns from: {@code return b * accum;}\n" +
                "</pre>"
        val summaryPow11 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code (((k & 1) == 0)): False}\n" +
                "returns from: {@code return ((k & 1) == 0) ? 1 : -1;}\n" +
                "</pre>"
        val summaryPow12 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code (k < Integer.SIZE): True},\n" +
                "    {@code (((k & 1) == 0)): False}\n" +
                "returns from: {@code return ((k & 1) == 0) ? (1 << k) : -(1 << k);}\n" +
                "</pre>"
        val summaryPow13 = "<pre>\n" +
                "Test executes conditions:\n" +
                "    {@code (((k & 1) == 0)): True}\n" +
                "returns from: {@code return ((k & 1) == 0) ? 1 : -1;}\n" +
                "</pre>"
        val summaryPow14 = "<pre>\n" +
                "Test iterates the loop {@code for(int accum = 1; ; k >>= 1)} twice,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    {@code (((k & 1) == 0)): True}\n" +
                ", returns from: {@code return b * accum;}\n" +
                "</pre>"
        checkOneArgument(
            IntMath::pow,
            summaryKeys = listOf(
                summaryPow1,
                summaryPow2,
                summaryPow3,
                summaryPow4,
                summaryPow5,
                summaryPow6,
                summaryPow7,
                summaryPow8,
                summaryPow9,
                summaryPow10,
                summaryPow11,
                summaryPow12,
                summaryPow13,
                summaryPow14
            )
        )
    }
}