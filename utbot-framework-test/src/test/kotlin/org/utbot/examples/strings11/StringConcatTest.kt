package org.utbot.examples.strings11

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

class StringConcatTest : UtValueTestCaseChecker(
    testClass = StringConcat::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testConcatArguments() {
        withoutConcrete {
            check(
                StringConcat::concatArguments,
                eq(1),
                { a, b, c, r -> "$a$b$c" == r }
            )
        }
    }

    @Test
    fun testConcatWithConstants() {
        withoutConcrete {
            check(
                StringConcat::concatWithConstants,
                eq(4),
                { a, r -> a == "head" && r == 1 },
                { a, r -> a == "body" && r == 2 },
                { a, r -> a == null && r == 3 },
                { a, r -> a != "head" && a != "body" && a != null && r == 4 },
            )
        }
    }

    @Disabled("Flickers too much with JVM 17")
    @Test
    fun testConcatWithPrimitives() {
        withoutConcrete {
            check(
                StringConcat::concatWithPrimitives,
                eq(1),
                { a, r -> "$a#4253.0" == r}
            )
        }
    }

    @Test
    fun testExceptionInToString() {
        withoutConcrete {
            checkWithException(
                StringConcat::exceptionInToString,
                ignoreExecutionsNumber,
                { t, r -> t.x == 42 && r.isException<IllegalArgumentException>() },
                { t, r -> t.x != 42 && r.getOrThrow() == "Test: x = ${t.x}!" },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testConcatWithField() {
        withoutConcrete {
            checkWithThis(
                StringConcat::concatWithField,
                eq(1),
                { o, a, r -> "$a${o.str}#" == r }
            )
        }
    }

    @Test
    fun testConcatWithPrimitiveWrappers() {
        withoutConcrete {
            check(
                StringConcat::concatWithPrimitiveWrappers,
                ignoreExecutionsNumber,
                { b, c, r -> b.toString().endsWith("4") && c == '2' && r == 1 },
                { _, c, r -> !c.toString().endsWith("42") && r == 2 },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testSameConcat() {
        withoutConcrete {
            check(
                StringConcat::sameConcat,
                ignoreExecutionsNumber,
                { a, b, r -> a == b && r == 0 },
                { a, b, r -> a != b && r == 1 },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testConcatStrangeSymbols() {
        withoutConcrete {
            check(
                StringConcat::concatStrangeSymbols,
                eq(1),
                { r -> r == "\u0000#\u0001!\u0002@\u0012\t" }
            )
        }
    }

}