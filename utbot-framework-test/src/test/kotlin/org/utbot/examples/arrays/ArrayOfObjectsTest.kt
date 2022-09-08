package org.utbot.examples.arrays

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class ArrayOfObjectsTest : UtValueTestCaseChecker(
    testClass = ArrayOfObjects::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testDefaultValues() {
        check(
            ArrayOfObjects::defaultValues,
            eq(1),
            { r -> r != null && r.single() == null },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testCreateArray() {
        check(
            ArrayOfObjects::createArray,
            eq(2),
            { _, _, length, _ -> length < 3 },
            { x, y, length, r ->
                require(r != null)

                val sizeConstraint = length >= 3 && r.size == length
                val contentConstraint = r.mapIndexed { i, elem -> elem.x == x + i && elem.y == y + i }.all { it }

                sizeConstraint && contentConstraint
            }
        )
    }

    @Test
    fun testCopyArray() {
        checkWithException(
            ArrayOfObjects::copyArray,
            ge(4),
            { a, r -> a == null && r.isException<NullPointerException>() },
            { a, r -> a.size < 3 && r.isException<IllegalArgumentException>() },
            { a, r -> a.size >= 3 && null in a && r.isException<NullPointerException>() },
            { a, r -> a.size >= 3 && r.getOrThrow().all { it.x == -1 && it.y == 1 } },
        )
    }

    @Test
    fun testCopyArrayMutation() {
        checkParamsMutations(
            ArrayOfObjects::copyArray,
            ignoreExecutionsNumber,
            { _, arrayAfter -> arrayAfter.all { it.x == -1 && it.y == 1 } }
        )
    }

    @Test
    fun testArrayWithSucc() {
        check(
            ArrayOfObjects::arrayWithSucc,
            eq(3),
            { length, _ -> length < 0 },
            { length, r -> length < 2 && r != null && r.size == length && r.all { it == null } },
            { length, r ->
                require(r != null)

                val sizeConstraint = length >= 2 && r.size == length
                val zeroElementConstraint = r[0] is ObjectWithPrimitivesClass && r[0].x == 2 && r[0].y == 4
                val firstElementConstraint = r[1] is ObjectWithPrimitivesClassSucc && r[1].x == 3

                sizeConstraint && zeroElementConstraint && firstElementConstraint
            },
        )
    }

    @Test
    fun testObjectArray() {
        check(
            ArrayOfObjects::objectArray,
            eq(5),
            { a, _, _ -> a == null },
            { a, _, r -> a != null && a.size != 2 && r == -1 },
            { a, o, _ -> a != null && a.size == 2 && o == null },
            { a, p, r -> a != null && a.size == 2 && p != null && p.x + 5 > 20 && r == 1 },
            { a, o, r -> a != null && a.size == 2 && o != null && o.x + 5 <= 20 && r == 0 },
        )
    }

    @Test
    fun testArrayOfArrays() {
        check(
            ArrayOfObjects::arrayOfArrays,
            between(4..5), // might be two ClassCastExceptions
            { a, _ -> a.any { it == null } },
            { a, _ -> a.any { it != null && it !is IntArray } },
            { a, r -> (a.all { it != null && it is IntArray && it.isEmpty() } || a.isEmpty()) && r == 0 },
            { a, r -> a.all { it is IntArray } && r == a.sumBy { (it as IntArray).sum() } },
            coverage = DoNotCalculate
        )
    }
}