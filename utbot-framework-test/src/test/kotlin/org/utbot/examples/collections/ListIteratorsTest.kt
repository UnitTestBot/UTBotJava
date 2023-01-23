package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.utbot.framework.plugin.api.CodegenLanguage
import kotlin.math.min
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber

// TODO failed Kotlin compilation (generics) SAT-1332
internal class ListIteratorsTest : UtValueTestCaseChecker(
    testClass = ListIterators::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReturnIterator() {
        withoutConcrete { // We need to check that a real class is returned but not `Ut` one
            check(
                ListIterators::returnIterator,
                ignoreExecutionsNumber,
                { l, r -> l.isEmpty() && r!!.asSequence().toList().isEmpty() },
                { l, r -> l.isNotEmpty() && r!!.asSequence().toList() == l },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testReturnListIterator() {
        withoutConcrete { // We need to check that a real class is returned but not `Ut` one
            check(
                ListIterators::returnListIterator,
                ignoreExecutionsNumber,
                { l, r -> l.isEmpty() && r!!.asSequence().toList().isEmpty() },
                { l, r -> l.isNotEmpty() && r!!.asSequence().toList() == l },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testIterate() {
        check(
            ListIterators::iterate,
            eq(3),
            { l, _ -> l == null },
            { l, result -> l.isEmpty() && result == l },
            { l, result -> l.isNotEmpty() && result == l },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIterateReversed() {
        check(
            ListIterators::iterateReversed,
            eq(3),
            { l, _ -> l == null },
            { l, result -> l.isEmpty() && result == l },
            { l, result -> l.isNotEmpty() && result == l.reversed() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIterateForEach() {
        check(
            ListIterators::iterateForEach,
            eq(4),
            { l, _ -> l == null },
            { l, result -> l.isEmpty() && result == 0 },
            { l, _ -> l.isNotEmpty() && l.any { it == null } },
            { l, result -> l.isNotEmpty() && result == l.sum() },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("Java 11 transition")
    fun testAddElements() {
        check(
            ListIterators::addElements,
            eq(5),
            { l, _, _ -> l == null },
            { l, _, result -> l != null && l.isEmpty() && result == l },
            { l, arr, _ -> l != null && l.size > 0 && arr == null },
            { l, arr, _ -> l != null && arr != null && l.isNotEmpty() && arr.isEmpty() },
            { l, arr, _ -> l != null && arr != null && l.size > arr.size },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testSetElements() {
        check(
            ListIterators::setElements,
            eq(5),
            { l, _, _ -> l == null },
            { l, _, result -> l != null && l.isEmpty() && result == l },
            { l, arr, _ -> l != null && arr != null && l.size > arr.size },
            { l, arr, _ -> l != null && l.size > 0 && arr == null },
            { l, arr, result -> l != null && arr != null && l.size <= arr.size && result == arr.asList().take(l.size) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemoveElements() {
        check(
            ListIterators::removeElements,
            ignoreExecutionsNumber, // the exact number of the executions depends on the decisions made by PathSelector
            // so we can have either six results or seven, depending on the [pathSelectorType]
            // from UtSettings
            { l, _, _ -> l == null },
            { l, i, _ -> l != null && i <= 0 },
            { l, i, _ -> l != null && l.isEmpty() && i > 0 },
            { l, i, _ -> l != null && i > 0 && l.subList(0, min(i, l.size)).any { it !is Int } },
            { l, i, _ -> l != null && i > 0 && l.subList(0, min(i, l.size)).any { it == null } },
            { l, i, _ -> l != null && l.isNotEmpty() && i > 0 },
            { l, i, result ->
                require(l != null)

                val precondition = l.isNotEmpty() && i > 0 && l.subList(0, i).all { it is Int }
                val postcondition = result == (l.subList(0, i - 1) + l.subList(min(l.size, i), l.size))

                precondition && postcondition
            },
        )
    }
}