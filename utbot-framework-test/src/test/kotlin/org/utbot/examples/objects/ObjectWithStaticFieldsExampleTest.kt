package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.findByName
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.singleValue
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ObjectWithStaticFieldsExampleTest : UtValueTestCaseChecker(testClass = ObjectWithStaticFieldsExample::class) {
    @Test
    fun testReadFromStaticArray() {
        checkStatics(
            ObjectWithStaticFieldsExample::readFromStaticArray,
            eq(6),
            { _, statics, _ -> statics.singleValue() == null },
            { _, statics, _ -> (statics.singleValue() as IntArray).size < 5 },
            { _, statics, _ -> (statics.singleValue() as IntArray)[1] != 1 },
            { _, statics, _ ->
                val array = statics.singleValue() as IntArray
                array[1] == 1 && array[2] != 2
            },
            { o, statics, _ ->
                val array = statics.singleValue() as IntArray
                o == null && array[1] == 1 && array[2] == 2
            },
            { o, statics, r ->
                val array = statics.singleValue() as IntArray
                r as ObjectWithStaticFieldsClass
                o != null && array[1] == 1 && array[2] == 2 && r.x == array[1] && r.y == array[2]
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testSetStaticField() {
        checkStaticsAfter(
            ObjectWithStaticFieldsExample::setStaticField,
            eq(4),
            { o, _, _ -> o == null },
            { o, _, _ -> o != null && o.x < 100 },
            { o, _, _ -> o != null && o.x >= 100 && o.y < 150 },
            { o, staticsAfter, r ->
                val staticValue = staticsAfter.singleValue() as Int

                val objectCondition = o != null && o.x >= 100 && o.y >= 150 && r?.x == o.x * o.y && r.y == o.y
                val staticCondition = staticValue == o.y * o.x
                val connection = r!!.x == staticValue

                objectCondition && staticCondition && connection
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetStaticField() {
        checkStatics(
            ObjectWithStaticFieldsExample::getStaticField,
            eq(3),
            { o, _, _ -> o == null },
            { o, statics, _ -> o != null && statics.singleValue() as Int != 3 },
            { o, statics, r -> o != null && statics.singleValue() as Int == 3 && r != null && r.x == 3 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetStaticFieldWithDefaultValue() {
        checkStatics(
            ObjectWithStaticFieldsExample::getStaticFieldWithDefaultValue,
            eq(1),
            { statics, r -> statics.singleValue() == r }
        )
    }

    @Test
    fun testStaticFieldInInvoke() {
        checkMutationsAndResult(
            ObjectWithStaticFieldsExample::staticFieldInInvoke,
            eq(1),
            { staticsBefore, staticsAfter, r ->
                val defaultValue = staticsBefore.findByName("defaultValue")
                staticsAfter.findByName("staticValue") == defaultValue && r == defaultValue
            }
        )
    }

    @Test
    fun testStaticFieldAfterStateInInvoke() {
        checkMutationsAndResult(
            ObjectWithStaticFieldsExample::staticFieldInInvoke,
            eq(1),
            { staticsBefore, staticsAfter, r ->
                val defaultValue = staticsBefore.findByName("defaultValue")
                val staticValue = staticsAfter.findByName("staticValue")

                defaultValue == staticValue && r == defaultValue
            }
        )
    }

    @Test
    fun testStaticFieldArrayMax() {
        checkMutationsAndResult(
            ObjectWithStaticFieldsExample::staticFieldArrayMax,
            eq(4),
            { staticsBefore, _, _ -> (staticsBefore.values.single().value as Int) < 0 },
            { staticsBefore, staticsAfter, _ ->
                val defaultValue = staticsBefore.findByName("defaultValue") as Int
                val staticArray = staticsAfter.findByName("staticArrayValue") as IntArray

                defaultValue == 0 && staticArray.isEmpty()
            },
            { staticsBefore, staticsAfter, r ->
                val defaultValue = staticsBefore.findByName("defaultValue") as Int
                val staticArray = staticsAfter.findByName("staticArrayValue") as IntArray

                val contentCondition = staticArray.zip(staticArray.indices).all { it.first == defaultValue + it.second }
                val maxValue = staticArray.maxOrNull()

                staticArray.size == 1 && contentCondition && maxValue == r
            },
            { staticsBefore, staticsAfter, r ->
                val defaultValue = staticsBefore.findByName("defaultValue") as Int
                val staticArray = staticsAfter.findByName("staticArrayValue") as IntArray

                val contentCondition = staticArray.zip(staticArray.indices).all { it.first == defaultValue + it.second }
                val maxValue = staticArray.maxOrNull()

                staticArray.size > 1 && contentCondition && maxValue == r
            },
        )
    }

    @Test
    fun testInitializedArrayWithCycle() {
        checkStatics(
            ObjectWithStaticFieldsExample::initializedArrayWithCycle,
            ignoreExecutionsNumber,
            { n, _, r -> n < 0 && r == Double.NEGATIVE_INFINITY },
            { _, statics, _ -> statics.singleValue() == null },
            { n, statics, _ -> n > 0 && (statics.singleValue() as IntArray).lastIndex < n },
            { n, statics, r ->
                r!!.toInt() == (1 until n).fold(1) { a, b -> a * b } * (statics.singleValue() as IntArray)[n]
            },
        )
    }

    @Test
    fun testBigStaticArray() {
        checkStatics(
            ObjectWithStaticFieldsExample::bigStaticArray,
            eq(3),
            { statics, _ -> statics.singleValue() == null },
            { statics, _ -> (statics.singleValue() as IntArray).lastIndex < 10 },
            { statics, r -> (statics.singleValue() as IntArray)[10] == r }
        )
    }

    @Test
    fun testModifyStatic() {
        checkStaticMethodMutationAndResult(
            ObjectWithStaticFieldsExample::modifyStatic,
            eq(2),
            { staticsBefore, staticsAfter, _ -> staticsBefore.singleValue() == 41 && staticsAfter.singleValue() == 42 },
            { staticsBefore, staticsAfter, _ ->
                staticsBefore.singleValue() != 41 && staticsAfter.singleValue() == staticsBefore.singleValue()
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testResetNonFinalFields() {
        checkMutationsAndResult(
            ObjectWithStaticFieldsExample::resetNonFinalFields,
            eq(2),
            { staticsBefore, staticsAfter, r ->
                staticsBefore.singleValue() == 42 && staticsAfter.singleValue() == 43 && r == 43
            },
            { staticsBefore, staticsAfter, r ->
                val value = staticsBefore.singleValue()
                value !in 42..43 && staticsAfter.singleValue() == value && r == value
            },
            coverage = DoNotCalculate
        )
    }
}