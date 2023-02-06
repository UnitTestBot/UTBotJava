package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.between
import org.utbot.testing.isException

internal class SimpleClassExampleTest : UtValueTestCaseChecker(testClass = SimpleClassExample::class) {
    @Test
    fun simpleConditionTest() {
        check(
            SimpleClassExample::simpleCondition,
            eq(4),
            { c, _ -> c == null }, // NPE
            { c, r -> c.a >= 5 && r == 3 },
            { c, r -> c.a < 5 && c.b <= 10 && r == 3 },
            { c, r -> c.a < 5 && c.b > 10 && r == 0 },
            coverage = DoNotCalculate // otherwise we overwrite original values
        )
    }

    /**
     * Additional bytecode instructions between IFs, because of random, makes different order of executing the branches,
     * that affects their number. Changing random seed in PathSelector can explore 6th branch
     *
     * @see multipleFieldAccessesTest
     */
    @Test
    fun singleFieldAccessTest() {
        check(
            SimpleClassExample::singleFieldAccess,
            between(5..6), // could be 6
            { c, _ -> c == null }, // NPE
            { c, r -> c.a == 3 && c.b != 5 && r == 2 },
            { c, r -> c.a == 3 && c.b == 5 && r == 1 },
            { c, r -> c.a == 2 && c.b != 3 && r == 2 },
            { c, r -> c.a == 2 && c.b == 3 && r == 0 }
        )
    }

    /**
     * Additional bytecode instructions between IFs, because of random, makes different order of executing the branches,
     * that affects their number
     */
    @Test
    fun multipleFieldAccessesTest() {
        check(
            SimpleClassExample::multipleFieldAccesses,
            eq(6),
            { c, _ -> c == null }, // NPE
            { c, r -> c.a != 2 && c.a != 3 && r == 2 }, // this one appears
            { c, r -> c.a == 3 && c.b != 5 && r == 2 },
            { c, r -> c.a == 3 && c.b == 5 && r == 1 },
            { c, r -> c.a == 2 && c.b != 3 && r == 2 },
            { c, r -> c.a == 2 && c.b == 3 && r == 0 }
        )
    }

    @Test
    fun immutableFieldAccessTest() {
        checkWithException(
            SimpleClassExample::immutableFieldAccess,
            eq(3),
            { c, r -> c == null && r.isException<NullPointerException>() },
            { c, r -> c.b == 10 && r.getOrNull() == 0 },
            { c, r -> c.b != 10 && r.getOrNull() == 1 }
        )
    }
}