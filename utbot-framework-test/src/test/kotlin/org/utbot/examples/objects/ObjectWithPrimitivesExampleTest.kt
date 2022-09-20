package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ObjectWithPrimitivesExampleTest : UtValueTestCaseChecker(testClass = ObjectWithPrimitivesExample::class) {
    @Test
    fun testMax() {
        checkWithException(
            ObjectWithPrimitivesExample::max,
            eq(7),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { _, snd, r -> snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && fst.x > snd.x && fst.y > snd.y && r.getOrNull()!! == fst },
            { fst, snd, r -> fst != null && snd != null && fst.x > snd.x && fst.y <= snd.y && r.getOrNull()!! == fst },
            { fst, snd, r -> fst != null && snd != null && fst.x < snd.x && fst.y < snd.y && r.getOrNull()!! == snd },
            { fst, snd, r -> fst != null && snd != null && fst.x == snd.x && r.getOrNull()!! == fst },
            { fst, snd, r -> fst != null && snd != null && fst.y == snd.y && r.getOrNull()!! == fst },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIgnoredInputParameters() {
        check(
            ObjectWithPrimitivesExample::ignoredInputParameters,
            eq(1),
            { fst, snd, r -> fst == null && snd == null && r != null }
        )
    }

    @Test
    fun testExample() {
        check(
            ObjectWithPrimitivesExample::example,
            eq(3),
            { v, _ -> v == null },
            { v, r -> v != null && v.x == 1 && r?.x == 1 },
            { v, r -> v != null && v.x != 1 && r?.x == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testExampleMutation() {
        checkParamsMutations(
            ObjectWithPrimitivesExample::example,
            ignoreExecutionsNumber,
            { valueBefore, valueAfter -> valueBefore.x != 0 && valueAfter.x == 1 }
        )
    }

    @Test
    fun testDefaultValueForSuperclassFields() {
        check(
            ObjectWithPrimitivesExample::defaultValueForSuperclassFields,
            eq(1),
            { r -> r != null && r.x == 0 && r.y == 0 && r.weight == 0.0 && r.valueByDefault == 5 && r.anotherX == 0 },
            coverage = atLeast(50)
        )
    }

    @Test
    @Disabled("TODO JIRA:1594")
    fun testCreateObject() {
        checkWithException(
            ObjectWithPrimitivesExample::createObject,
            eq(3),
            { _, _, o, r -> o == null && r.isException<NullPointerException>() },
            { _, _, o, r -> o != null && o.weight < 0 && r.isException<IllegalArgumentException>() },
            { a, b, o, r ->
                val result = r.getOrNull()!!

                val objectConstraint = o != null && (o.weight >= 0 || o.weight.isNaN())
                val resultConstraint = result.x == a + 5 && result.y == b + 6
                val postcondition = result.weight == o.weight || result.weight.isNaN() && o.weight.isNaN()

                objectConstraint && resultConstraint && postcondition
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testMemory() {
        checkWithException(
            ObjectWithPrimitivesExample::memory,
            eq(4),
            { o, v, r -> o == null && v > 0 && r.isException<NullPointerException>() },
            { o, v, r ->
                val resultValue = r.getOrNull()
                val objectToCompare = if (resultValue is ObjectWithPrimitivesClassSucc) {
                    ObjectWithPrimitivesClassSucc(1, 2, 1.2, resultValue.anotherX)
                } else {
                    ObjectWithPrimitivesClass(1, 2, 1.2)
                }
                objectToCompare.valueByDefault = resultValue!!.valueByDefault

                o != null && v > 0 && resultValue == objectToCompare
            },
            { o, v, r -> o == null && v <= 0 && r.isException<NullPointerException>() },
            { o, v, r ->
                val resultValue = r.getOrNull()
                val objectToCompare = if (resultValue is ObjectWithPrimitivesClassSucc) {
                    ObjectWithPrimitivesClassSucc(-1, -2, -1.2, resultValue.anotherX)
                } else {
                    ObjectWithPrimitivesClass(-1, -2, -1.2)
                }
                objectToCompare.valueByDefault = resultValue!!.valueByDefault

                o != null && v <= 0 && resultValue == objectToCompare
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareWithNull() {
        check(
            ObjectWithPrimitivesExample::compareWithNull,
            eq(3),
            { fst, _, r -> fst == null && r == 1 },
            { fst, snd, r -> fst != null && snd == null && r == 2 },
            { fst, snd, r -> fst != null && snd != null && r == 3 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoNullObjects() {
        check(
            ObjectWithPrimitivesExample::compareTwoNullObjects,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testNullExample() {
        check(
            ObjectWithPrimitivesExample::nullExample,
            eq(4),
            { o, _ -> o == null },
            { o, r -> o != null && o.x != 0 && r != null },
            { o, r -> o != null && o.x == 0 && o.y != 0 && r != null },
            { o, r -> o != null && o.x == 0 && o.y == 0 && r == null },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoOuterObjects() {
        checkWithException(
            ObjectWithPrimitivesExample::compareTwoOuterObjects,
            eq(4),
            { x, _, r -> x == null && r.isException<NullPointerException>() },
            { x, y, r -> x != null && y == null && r.isException<NullPointerException>() },
            { x, y, r -> x != null && y != null && x === y && r.getOrNull() == true },
            { x, y, r -> x != null && y != null && x !== y && r.getOrNull() == false }
        )
    }

    @Test
    fun testCompareObjectWithArgument() {
        check(
            ObjectWithPrimitivesExample::compareObjectWithArgument,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoDifferentObjects() {
        check(
            ObjectWithPrimitivesExample::compareTwoDifferentObjects,
            eq(1),
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testCompareTwoIdenticalObjectsFromArguments() {
        checkWithException(
            ObjectWithPrimitivesExample::compareTwoIdenticalObjectsFromArguments,
            eq(4),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { _, snd, r -> snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 1 },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoRefEqualObjects() {
        check(
            ObjectWithPrimitivesExample::compareTwoRefEqualObjects,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetOrDefault() {
        checkWithException(
            ObjectWithPrimitivesExample::getOrDefault,
            ignoreExecutionsNumber,
            { _, d, r -> d == null && r.isException<NullPointerException>() },
            { _, d, r -> d != null && d.x == 0 && d.y == 0 && r.isException<IllegalArgumentException>() },
            { o, d, r -> o == null && (d.x != 0 || d.y != 0) && r.getOrNull() == d },
            { o, d, r -> o != null && (d.x != 0 || d.y != 0) && r.getOrNull() == o },
        )
    }

    @Test
    fun testInheritorsFields() {
        checkWithException(
            ObjectWithPrimitivesExample::inheritorsFields,
            eq(3),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateWithConstructor() {
        check(
            ObjectWithPrimitivesExample::createWithConstructor,
            eq(1),
            { x, y, r -> r != null && r.x == x + 1 && r.y == y + 2 && r.weight == 3.3 }
        )
    }

    @Test
    fun testCreateWithSuperConstructor() {
        check(
            ObjectWithPrimitivesExample::createWithSuperConstructor,
            eq(1),
            { x, y, anotherX, r ->
                r != null && r.x == x + 1 && r.y == y + 2 && r.weight == 3.3 && r.anotherX == anotherX + 4
            }
        )
    }

    @Test
    fun testFieldWithDefaultValue() {
        check(
            ObjectWithPrimitivesExample::fieldWithDefaultValue,
            eq(1),
            { x, y, r -> r != null && r.x == x && r.y == y && r.weight == 3.3 && r.valueByDefault == 5 }
        )
    }

    @Test
    fun testValueByDefault() {
        check(
            ObjectWithPrimitivesExample::valueByDefault,
            eq(1),
            { r -> r == 5 }
        )
    }
}
