package org.utbot.examples.structures

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import kotlin.math.min
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class MinStackExampleTest : UtValueTestCaseChecker(testClass = MinStackExample::class) {
    @Test
    fun testCreate() {
        check(
            MinStackExample::create,
            eq(3),
            { initialValues, _ -> initialValues == null },
            { initialValues, _ -> initialValues != null && initialValues.size < 3 },
            { initialValues, result ->
                require(initialValues != null && result != null)

                val stackExample = MinStackExample().create(initialValues)
                val initialSize = initialValues.size

                val sizesConstraint = initialSize >= 3 && result.size == 4
                val stacksSize = stackExample.stack.take(initialSize) == result.stack.take(initialSize)
                val minStackSize = stackExample.minStack.take(initialSize) == result.minStack.take(initialSize)

                sizesConstraint && stacksSize && minStackSize
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAddSingleValue() {
        check(
            MinStackExample::addSingleValue,
            eq(3),
            { initialValues, _ -> initialValues == null },
            { initialValues, _ -> initialValues != null && initialValues.isEmpty() },
            { initialValues, result ->
                require(initialValues != null && result != null)

                val sizeConstraint = initialValues.isNotEmpty()
                val firstElementConstraint = result.stack.first() == initialValues.first()
                val secondElementConstraint = result.stack[1] == initialValues.first() - 100

                sizeConstraint && firstElementConstraint && secondElementConstraint
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetMinValue() {
        check(
            MinStackExample::getMinValue,
            eq(3),
            { initialValues, _ -> initialValues == null },
            { initialValues, result -> initialValues != null && initialValues.isEmpty() && result == -1L },
            { initialValues, result ->
                initialValues != null && initialValues.isNotEmpty() && result == min(-1L, initialValues.minOrNull()!!)
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemoveValue() {
        check(
            MinStackExample::removeValue,
            eq(4),
            { initialValues, _ -> initialValues == null },
            { initialValues, _ -> initialValues != null && initialValues.isEmpty() },
            { initialValues, result ->
                initialValues != null && initialValues.size == 1 && result != null && result.size == initialValues.size - 1
            },
            { initialValues, result ->
                initialValues != null && initialValues.size > 1 && result != null && result.size == initialValues.size - 1
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testConstruct() {
        check(
            MinStackExample::construct,
            between(3..4),
            { values, _ -> values == null },
            { values, result -> values != null && values.isEmpty() && result != null && result.size == 0 },
            { values, result ->
                require(values != null && result != null)

                val stackExample = MinStackExample().construct(values)

                val sizeConstraint = values.isNotEmpty() && result.size == values.size
                val stackSize = stackExample.stack.take(values.size) == result.stack.take(values.size)
                val valueConstraint = stackExample.minStack.take(values.size) == result.minStack.take(values.size)

                sizeConstraint && stackSize && valueConstraint
            },
            coverage = DoNotCalculate
        )
    }
}