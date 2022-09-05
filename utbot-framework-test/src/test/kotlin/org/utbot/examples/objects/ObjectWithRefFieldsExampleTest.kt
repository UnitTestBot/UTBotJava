package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ObjectWithRefFieldsExampleTest : UtValueTestCaseChecker(testClass = ObjectWithRefFieldExample::class) {
    @Test
    fun testDefaultValue() {
        check(
            ObjectWithRefFieldExample::defaultValue,
            eq(1),
            { r -> r != null && r.x == 0 && r.y == 0 && r.weight == 0.0 && r.arrayField == null && r.refField == null },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testWriteToRefTypeField() {
        check(
            ObjectWithRefFieldExample::writeToRefTypeField,
            eq(4),
            { _, v, _ -> v != 42 },
            { o, v, _ -> v == 42 && o == null },
            { o, v, _ -> v == 42 && o != null && o.refField != null },
            { o, v, r ->
                v == 42 && o != null && o.refField == null && r != null && r.refField.a == v && r.refField.b == 2 * v
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testDefaultFieldValues() {
        check(
            ObjectWithRefFieldExample::defaultFieldValues,
            eq(1),
            { r ->
                r != null && r.x == 0 && r.y == 0 && r.weight == 0.0 && r.refField == null && r.arrayField == null
            }
        )
    }

    @Test
    fun testReadFromRefTypeField() {
        check(
            ObjectWithRefFieldExample::readFromRefTypeField,
            eq(4),
            { o, _ -> o == null },
            { o, _ -> o != null && o.refField == null },
            { o, r -> o?.refField != null && o.refField.a <= 0 && r == -1 },
            { o, r -> o?.refField != null && o.refField.a > 0 && o.refField.a == r }
        )
    }

    @Test
    fun testWriteToArrayField() {
        check(
            ObjectWithRefFieldExample::writeToArrayField,
            eq(3),
            { _, length, _ -> length < 3 },
            { o, length, _ -> length >= 3 && o == null },
            { o, length, r ->
                require(r != null)

                val array = r.arrayField

                val preconditions = length >= 3 && o != null
                val contentConstraint = array.dropLast(1) == (1 until length).toList() && array.last() == 100

                preconditions && contentConstraint
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReadFromArrayField() {
        check(
            ObjectWithRefFieldExample::readFromArrayField,
            eq(5),
            { o, _, _ -> o == null },
            { o, _, _ -> o != null && o.arrayField == null },
            { o, _, _ -> o?.arrayField != null && o.arrayField.size < 3 },
            { o, v, r -> o?.arrayField != null && o.arrayField.size >= 3 && o.arrayField[2] == v && r == 1 },
            { o, v, r -> o?.arrayField != null && o.arrayField.size >= 3 && o.arrayField[2] != v && r == 2 }
        )
    }

    @Test
    fun testCompareTwoDifferentObjectsFromArguments() {
        check(
            ObjectWithRefFieldExample::compareTwoDifferentObjectsFromArguments,
            ignoreExecutionsNumber,
            { fst, _, _ -> fst == null },
            { fst, snd, _ -> fst != null && fst.x > 0 && snd == null },
            { fst, snd, _ -> fst != null && fst.x <= 0 && snd == null },
            { fst, snd, r -> fst != null && snd != null && fst.x > 0 && snd.x < 0 && r == 1 },
            { fst, snd, r -> fst != null && snd != null && ((fst.x > 0 && snd.x >= 0) || fst.x <= 0) && fst === snd && r == 2 },
            { fst, snd, r -> fst != null && snd != null && (fst.x <= 0 || (fst.x > 0 && snd.x >= 0)) && fst !== snd && r == 3 },
            coverage = atLeast(87)
        )
    }

    @Test
    fun testCompareTwoObjectsWithNullRefField() {
        checkWithException(
            ObjectWithRefFieldExample::compareTwoObjectsWithNullRefField,
            eq(4),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 1 /* && fst == snd by ref */ },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 2 /* && fst != snd by ref */ },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoObjectsWithDifferentRefField() {
        checkWithException(
            ObjectWithRefFieldExample::compareTwoObjectsWithDifferentRefField,
            eq(4),
            { fst, _, _, r -> fst == null && r.isException<NullPointerException>() },
            { fst, snd, _, r -> fst != null && snd == null && r.isException<NullPointerException>() },
            { fst, snd, _, r -> fst != null && snd != null && r.getOrNull() == 1 /* fst == snd by ref */ },
            { fst, snd, _, r -> fst != null && snd != null && r.getOrNull() == 2 /* fst != snd by ref */ },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCompareTwoObjectsWithTheDifferentRefField() {
        checkWithException(
            ObjectWithRefFieldExample::compareTwoObjectsWithDifferentRefField,
            eq(4),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && fst.refField === snd.refField && r.getOrNull() == true },
            { fst, snd, r -> fst != null && snd != null && fst.refField !== snd.refField && r.getOrNull() == false }
        )
    }

    @Test
    fun testCompareTwoObjectsWithTheSameRefField() {
        checkWithException(
            ObjectWithRefFieldExample::compareTwoObjectsWithTheSameRefField,
            eq(4),
            { fst, _, r -> fst == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd == null && r.isException<NullPointerException>() },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 1 /* && fst == snd by ref */ },
            { fst, snd, r -> fst != null && snd != null && r.getOrNull() == 2 /* && fst != snd by ref */ },
            coverage = DoNotCalculate
        )
    }
}