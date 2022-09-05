package org.utbot.examples.casts

import org.junit.jupiter.api.Disabled
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation (generics) SAT-1332
//TODO: SAT-1487 calculate coverage for all methods of this test class
internal class ArrayCastExampleTest : UtValueTestCaseChecker(
    testClass = ArrayCastExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testCastToAncestor() {
        check(
            ArrayCastExample::castToAncestor,
            eq(2),
            { a, r -> a == null && r != null && r is Array<CastClass> },
            { a, r -> a != null && r != null && r.isArrayOf<CastClassFirstSucc>() }
        )
    }

    @Test
    fun testClassCastException() {
        check(
            ArrayCastExample::classCastException,
            eq(3),
            { a, r -> a == null && r != null && r.isEmpty() },
            { a, _ -> !a.isArrayOf<CastClassFirstSucc>() },
            { a, r -> a.isArrayOf<CastClassFirstSucc>() && r != null && r.isArrayOf<CastClassFirstSucc>() },
        )
    }

    @Test
    fun testNullCast() {
        check(
            ArrayCastExample::nullCast,
            eq(2),
            { a, r -> a != null && r == null },
            { a, r -> a == null && r == null }
        )
    }

    @Test
    fun testNullArray() {
        check(
            ArrayCastExample::nullArray,
            eq(1),
            { r -> r == null }
        )
    }

    @Test
    fun testSuccessfulExampleFromJLS() {
        check(
            ArrayCastExample::successfulExampleFromJLS,
            eq(1),
            { r ->
                require(r != null)

                val sizeConstraint = r.size == 4
                val typeConstraint = r[0] is ColoredPoint && r[1] is ColoredPoint
                val zeroElementConstraint = r[0].x == 2 && r[0].y == 2 && r[0].color == 12
                val firstElementConstraint = r[1].x == 4 && r[1].y == 5 && r[1].color == 24

                sizeConstraint && typeConstraint && zeroElementConstraint && firstElementConstraint
            }
        )
    }

    @Test
    fun testCastAfterStore() {
        check(
            ArrayCastExample::castAfterStore,
            eq(5),
            { a, _ -> a == null },
            { a, _ -> a.isEmpty() },
            { a, _ -> a.isNotEmpty() && !a.isArrayOf<ColoredPoint>() },
            { a, _ -> a.isArrayOf<ColoredPoint>() && a.size == 1 },
            { a, r ->
                require(r != null)

                val sizeConstraint = a.size >= 2
                val typeConstraint = a.isArrayOf<ColoredPoint>() && r.isArrayOf<ColoredPoint>()
                val zeroElementConstraint = r[0].color == 12 && r[0].x == 1 && r[0].y == 2
                val firstElementConstraint = r[1].color == 14 && r[1].x == 2 && r[1].y == 3

                sizeConstraint && typeConstraint && zeroElementConstraint && firstElementConstraint
            }
        )
    }

    @Test
    fun testCastFromObject() {
        check(
            ArrayCastExample::castFromObject,
            eq(3),
            { a, _ -> a !is Array<*> || !a.isArrayOf<CastClassFirstSucc>() },
            { a, r -> a == null && r != null && r.isArrayOf<CastClassFirstSucc>() && r.isEmpty() },
            { a, r -> a is Array<*> && a.isArrayOf<CastClassFirstSucc>() && r != null && r.isArrayOf<CastClassFirstSucc>() },
        )
    }

    @Test
    fun testCastFromObjectToPrimitivesArray() {
        check(
            ArrayCastExample::castFromObjectToPrimitivesArray,
            eq(2),
            { array, r -> array is IntArray && array.size > 0 && r is IntArray && array contentEquals r },
            { array, r -> array != null && array !is IntArray && r !is IntArray },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCastsChainFromObject() {
        check(
            ArrayCastExample::castsChainFromObject,
            eq(8),
            { a, r -> a == null && r == null },
            { a, _ -> a !is Array<*> || !a.isArrayOf<Point>() },
            { a, r -> a is Array<*> && a.isArrayOf<Point>() && a.isEmpty() && r == null },
            { a, _ -> a is Array<*> && a.isArrayOf<Point>() && a.isNotEmpty() && a[0] == null },
            { a, _ -> a is Array<*> && a.isArrayOf<Point>() && !a.isArrayOf<ColoredPoint>() && (a[0] as Point).x == 1 },
            { a, _ -> a is Array<*> && a.isArrayOf<Point>() && !a.isArrayOf<ColoredPoint>() && (a[0] as Point).x != 1 },
            { a, r -> a is Array<*> && a.isArrayOf<ColoredPoint>() && (a[0] as Point).x == 1 && r != null && r[0].x == 10 },
            { a, r -> a is Array<*> && a.isArrayOf<ColoredPoint>() && (a[0] as Point).x != 1 && r != null && r[0].x == 5 },
        )
    }

    @Test
    fun testCastFromCollections() {
        check(
            ArrayCastExample::castFromCollections,
            eq(3),
            { c, r -> c == null && r == null },
            { c, r -> c != null && c is List<*> && r is List<*> },
            { c, _ -> c is Collection<*> && c !is List<*> },
            coverage = DoNotCalculate,
        )
    }

    @Test
    fun testCastFromIterable() {
        check(
            ArrayCastExample::castFromIterable,
            eq(3),
            { i, r -> i == null && r == null },
            { i, r -> i is List<*> && r is List<*> },
            { i, _ -> i is Iterable<*> && i !is List<*> },
            coverage = DoNotCalculate,
        )
    }

    @Test
    @Disabled("Fix Traverser.findInvocationTargets to exclude non-exported classes / provide good classes")
    fun testCastFromIterableToCollection() {
        check(
            ArrayCastExample::castFromIterableToCollection,
            eq(3),
            { i, r -> i == null && r == null },
            { i, r -> i is Collection<*> && r is Collection<*> },
            { i, _ -> i is Iterable<*> && i !is Collection<*> },
            coverage = DoNotCalculate,
        )
    }
}