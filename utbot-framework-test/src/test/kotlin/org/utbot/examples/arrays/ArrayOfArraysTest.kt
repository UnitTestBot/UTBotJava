package org.utbot.examples.arrays

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.atLeast
import org.utbot.examples.casts.ColoredPoint
import org.utbot.examples.casts.Point
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutMinimization

@Suppress("NestedLambdaShadowedImplicitParameter")
internal class ArrayOfArraysTest : UtValueTestCaseChecker(testClass = ArrayOfArrays::class) {
    @Test
    fun testDefaultValues() {
        check(
            ArrayOfArrays::defaultValues,
            eq(1),
            { r -> r != null && r.single() == null },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testExample() {
        check(
            ArrayOfArrays::sizesWithoutTouchingTheElements,
            eq(1),
            { r -> r != null && r.size == 10 && r.all { it.size == 3 && it.all { it == 0 } } },
        )
    }

    @Test
    fun testDefaultValuesWithoutLastDimension() {
        check(
            ArrayOfArrays::defaultValuesWithoutLastDimension,
            eq(1),
            { r -> r != null && r.all { it.size == 4 && it.all { it.size == 4 && it.all { it == null } } } },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testCreateNewMultiDimensionalArray() {
        withoutMinimization { // TODO: JIRA:1506
            check(
                ArrayOfArrays::createNewMultiDimensionalArray,
                eq(4),
                { i, j, _ -> i < 0 || j < 0 },
                { i, j, r -> i == 0 && j >= 0 && r != null && r.size == 2 && r.all { it.isEmpty() } },
                { i, j, r ->
                    val indicesConstraint = i > 0 && j == 0
                    val arrayPropertiesConstraint = r != null && r.size == 2
                    val arrayContentConstraint = r?.all { it.size == i && it.all { it.isEmpty() } } ?: false

                    indicesConstraint && arrayPropertiesConstraint && arrayContentConstraint
                },
                { i, j, r ->
                    val indicesConstraint = i > 0 && j > 0
                    val arrayPropertiesConstraint = r != null && r.size == 2
                    val arrayContentConstraint =
                        r?.all {
                            it.size == i && it.all {
                                it.size == j && it.all {
                                    it.size == 3 && it.all { it == 0 }
                                }
                            }
                        }

                    indicesConstraint && arrayPropertiesConstraint && (arrayContentConstraint ?: false)
                }
            )
        }
    }

    @Test
    fun testDefaultValuesWithoutTwoDimensions() {
        check(
            ArrayOfArrays::defaultValuesWithoutTwoDimensions,
            eq(2),
            { i, r -> i < 2 && r == null },
            { i, r -> i >= 2 && r != null && r.all { it.size == i && it.all { it == null } } },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testDefaultValuesNewMultiArray() {
        check(
            ArrayOfArrays::defaultValuesNewMultiArray,
            eq(1),
            { r -> r != null && r.single().single().single() == 0 },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testSimpleExample() {
        check(
            ArrayOfArrays::simpleExample,
            eq(7),
            { m, r -> m.size >= 3 && m[1] === m[2] && r == null },
            { m, r -> m.size >= 3 && m[1] !== m[2] && m[0] === m[2] && r == null },
            { m, _ -> m.size >= 3 && m[1].size < 2 },
            { m, _ -> m.size >= 3 && m[1][1] == 1 && m[2].size < 3 },
            { m, r -> m.size >= 3 && m[1][1] == 1 && m[2].size >= 3 && r != null && r[2][2] == 2 },
            { m, _ -> m.size >= 3 && m[1][1] != 1 && m[2].size < 3 },
            { m, r -> m.size >= 3 && m[1][1] != 1 && m[2].size >= 3 && r != null && r[2][2] == -2 },
            coverage = DoNotCalculate // because of assumes
        )
    }

    @Test
    fun testSimpleExampleMutation() {
        checkParamsMutations(
            ArrayOfArrays::simpleExample,
            eq(7),
            { matrixBefore, matrixAfter -> matrixBefore[1][1] == 1 && matrixAfter[2][2] == 2 },
            { matrixBefore, matrixAfter -> matrixBefore[1][1] != 1 && matrixAfter[2][2] == -2 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIsIdentityMatrix() {
        withoutMinimization {
            check(
                ArrayOfArrays::isIdentityMatrix,
                eq(9),
                { m, _ -> m == null },
                { m, _ -> m.size < 3 },
                { m, _ -> m.size >= 3 && m.any { it == null } },
                { m, r -> m.size >= 3 && m.any { it.size != m.size } && r == false },
                { m, r -> m.size >= 3 && m.size == m[0].size && m[0][0] != 1 && r == false },
                { m, r -> m.size >= 3 && m.size == m[0].size && m[0][0] == 1 && m[0][1] != 0 && r == false },
                { m, r -> m.size >= 3 && m.size == m[0].size && m[0][0] == 1 && m[0][1] == 0 && m[0][2] != 0 && r == false },
                { m, r ->
                    val sizeConstraints = m.size >= 3 && m.size == m[0].size
                    val valueConstraint = m[0][0] == 1 && m[0].drop(1).all { it == 0 }
                    val resultCondition = (m[1] == null && r == null) || (m[1]?.size != m.size && r == false)

                    sizeConstraints && valueConstraint && resultCondition
                },
                { m, r ->
                    val sizeConstraint = m.size >= 3 && m.size == m.first().size
                    val contentConstraint =
                        m.indices.all { i ->
                            m.indices.all { j ->
                                (i == j && m[i][j] == 1) || (i != j && m[i][j] == 0)
                            }
                        }

                    sizeConstraint && contentConstraint && r == true
                },
            )
        }
    }

    @Test
    fun testCreateNewThreeDimensionalArray() {
        check(
            ArrayOfArrays::createNewThreeDimensionalArray,
            eq(2),
            { length, _, r -> length != 2 && r != null && r.isEmpty() },
            { length, constValue, r ->
                val sizeConstraint = length == 2 && r != null && r.size == length
                val contentConstraint =
                    r!!.all {
                        it.size == length && it.all {
                            it.size == length && it.all { it == constValue + 1 }
                        }
                    }

                sizeConstraint && contentConstraint
            }
        )
    }

    @Test
    fun testReallyMultiDimensionalArray() {
        check(
            ArrayOfArrays::reallyMultiDimensionalArray,
            eq(8),
            { a, _ -> a == null },
            { a, _ -> a.size < 2 },
            { a, _ -> a.size >= 2 && a[1] == null },
            { a, _ -> a.size >= 2 && a[1].size < 3 },
            { a, _ -> a.size >= 2 && a[1].size >= 3 && a[1][2] == null },
            { a, _ -> a.size >= 2 && a[1].size >= 3 && a[1][2].size < 4 },
            { a, r ->
                val sizeConstraint = a.size >= 2 && a[1].size >= 3 && a[1][2].size >= 4
                val valueConstraint = a[1][2][3] == 12345 && r != null && r[1][2][3] == -12345

                sizeConstraint && valueConstraint
            },
            { a, r ->
                val sizeConstraint = a.size >= 2 && a[1].size >= 3 && a[1][2].size >= 4
                val valueConstraint = a[1][2][3] != 12345 && r != null && r[1][2][3] == 12345

                sizeConstraint && valueConstraint
            },
        )
    }

    @Test
    fun testReallyMultiDimensionalArrayMutation() {
        checkParamsMutations(
            ArrayOfArrays::reallyMultiDimensionalArray,
            ignoreExecutionsNumber,
            { arrayBefore, arrayAfter -> arrayBefore[1][2][3] != 12345 && arrayAfter[1][2][3] == 12345 },
            { arrayBefore, arrayAfter -> arrayBefore[1][2][3] == 12345 && arrayAfter[1][2][3] == -12345 },
        )
    }

    @Test
    fun testMultiDimensionalObjectsArray() {
        check(
            ArrayOfArrays::multiDimensionalObjectsArray,
            eq(4),
            { a, _ -> a == null },
            { a, _ -> a.isEmpty() },
            { a, _ -> a.size == 1 },
            { a, r ->
                require(r != null && r[0] != null && r[1] != null)

                val propertiesConstraint = a.size > 1
                val zeroElementConstraints = r[0] is Array<*> && r[0].isArrayOf<ColoredPoint>() && r[0].size == 2
                val firstElementConstraints = r[1] is Array<*> && r[1].isArrayOf<Point>() && r[1].size == 1

                propertiesConstraint && zeroElementConstraints && firstElementConstraints
            },
        )
    }

    @Test
    fun testMultiDimensionalObjectsArrayMutation() {
        checkParamsMutations(
            ArrayOfArrays::multiDimensionalObjectsArray,
            ignoreExecutionsNumber,
            { _, arrayAfter ->
                arrayAfter[0] is Array<*> && arrayAfter[0].isArrayOf<ColoredPoint>() && arrayAfter[0].size == 2
            },
            { _, arrayAfter ->
                arrayAfter[1] is Array<*> && arrayAfter[1].isArrayOf<Point>() && arrayAfter[1].size == 1
            },
        )
    }

    @Test
    fun testFillMultiArrayWithArray() {
        check(
            ArrayOfArrays::fillMultiArrayWithArray,
            eq(3),
            { v, _ -> v == null },
            { v, r -> v.size < 2 && r != null && r.isEmpty() },
            { v, r -> v.size >= 2 && r != null && r.all { a -> a.toList() == v.mapIndexed { i, elem -> elem + i } } }
        )
    }

    @Test
    fun testFillMultiArrayWithArrayMutation() {
        checkParamsMutations(
            ArrayOfArrays::fillMultiArrayWithArray,
            ignoreExecutionsNumber,
            { valueBefore, valueAfter -> valueAfter.withIndex().all { it.value == valueBefore[it.index] + it.index } }
        )
    }

    @Test
    fun testArrayWithItselfAnAsElement() {
        check(
            ArrayOfArrays::arrayWithItselfAnAsElement,
            eq(2),
            coverage = atLeast(percents = 94)
            // because of the assumption
        )
    }
}