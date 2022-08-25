package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
class MapValuesTest : UtValueTestCaseChecker(
    testClass = MapValues::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testRemoveFromValues() {
        withoutMinimization { // TODO: JIRA:1506
            checkWithException(
                MapValues::removeFromValues,
                ignoreExecutionsNumber,
                { map, _, result -> map == null && result.isException<NullPointerException>() },
                { map, i, result -> i !in map.values && result.getOrNull() == map },
                { map, i, result ->
                    val resultMap = result.getOrNull()!!

                    val iInMapValues = i in map.values
                    val iWasDeletedFromValues =
                        resultMap.values.filter { it == i }.size == map.values.filter { it == i }.size - 1

                    val firstKeyAssociatedWithI = map.keys.first { map[it] == i }
                    val firstKeyAssociatedWIthIWasDeleted = firstKeyAssociatedWithI !in resultMap.keys

                    val getCountExceptI: Collection<Int?>.() -> Map<Int, Int> =
                        { this.filter { it != i }.filterNotNull().groupingBy { it }.eachCount() }
                    val mapContainsAllValuesFromResult =
                        map.values.getCountExceptI() == resultMap.values.getCountExceptI()

                    iInMapValues && iWasDeletedFromValues && firstKeyAssociatedWIthIWasDeleted && mapContainsAllValuesFromResult
                },
            )
        }
    }

    @Test
    fun testAddToValues() {
        checkWithException(
            MapValues::addToValues,
            between(2..4),
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map != null && result.isException<UnsupportedOperationException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetFromValues() {
        withoutMinimization {
            check(
                MapValues::getFromValues,
                ignoreExecutionsNumber,
                { map, _, _ -> map == null },
                { map, i, result -> i !in map.values && result == 1 },
                { map, i, result -> i in map.values && result == 1 },
                coverage = AtLeast(90) // unreachable else branch in MUT
            )
        }
    }

    @Test
    fun testIteratorHasNext() {
        check(
            MapValues::iteratorHasNext,
            between(3..4),
            { map, _ -> map == null },
            { map, result -> map.values.isEmpty() && result == 0 },
            { map, result -> map.values.isNotEmpty() && result == map.values.size },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIteratorNext() {
        checkWithException(
            MapValues::iteratorNext,
            between(3..4),
            { map, result -> map == null && result.isException<NullPointerException>() },
            // We might lose this branch depending on the order of the exploration since
            // we do not register wrappers, and, therefore, do not try to cover all of their branches
            // { map, result -> map != null && map.values.isEmpty() && result.isException<NoSuchElementException>() },
            { map, result -> map != null && map.values.first() == null && result.isException<NullPointerException>() },
            // as map is LinkedHashmap by default this matcher would be correct
            { map, result -> map != null && map.values.isNotEmpty() && result.getOrNull() == map.values.first() },
        )
    }

    @Test
    fun testIteratorRemove() {
        checkWithException(
            MapValues::iteratorRemove,
            between(3..4),
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map.values.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, result ->
                val resultMap = result.getOrNull()!!
                val firstValue = map.values.first()

                val getCountsExceptFirstValue: Collection<Int?>.() -> Map<Int, Int> =
                    { this.filter { it != firstValue }.filterNotNull().groupingBy { it }.eachCount() }
                val mapContainsAllValuesFromResult =
                    map.values.getCountsExceptFirstValue() == resultMap.values.getCountsExceptFirstValue()

                val firstValueWasDeleted =
                    resultMap.values.filter { it == firstValue }.size == map.values.filter { it == firstValue }.size - 1

                val keyAssociatedWithFirstValueWasDeleted =
                    map.keys.first { map[it] == firstValue } !in resultMap.keys

                mapContainsAllValuesFromResult && firstValueWasDeleted && keyAssociatedWithFirstValueWasDeleted
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIteratorRemoveOnIndex() {
        checkWithException(
            MapValues::iteratorRemoveOnIndex,
            ge(5),
            { _, i, result -> i == 0 && result.isSuccess && result.getOrNull() == null },
            { map, _, result -> map == null && result.isException<NullPointerException>() },
            { map, i, result -> map != null && i < 0 && result.isException<IllegalStateException>() },
            { map, i, result -> i > map.values.size && result.isException<NoSuchElementException>() },
            { map, i, result ->
                val iInIndexRange = i in 1..map.size
                val ithValue = map.values.toList()[i - 1]
                val resultMap = result.getOrNull()!!

                val getCountsExceptIthValue: Collection<Int?>.() -> Map<Int, Int> =
                    { this.filter { it != ithValue }.filterNotNull().groupingBy { it }.eachCount() }
                val mapContainsAllValuesFromResult =
                    map.values.getCountsExceptIthValue() == resultMap.values.getCountsExceptIthValue()
                val ithValueWasDeleted =
                    resultMap.values.filter { it == ithValue }.size == map.values.filter { it == ithValue }.size - 1
                val keyAssociatedWIthIthValueWasDeleted =
                    map.keys.filter { map[it] == ithValue }.any { it !in resultMap.keys }

                iInIndexRange && mapContainsAllValuesFromResult && ithValueWasDeleted && keyAssociatedWIthIthValueWasDeleted
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIterateForEach() {
        check(
            MapValues::iterateForEach,
            between(3..5),
            { map, _ -> map == null },
            { map, _ -> null in map.values },
            { map, result -> map != null && result == map.values.sum() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIterateWithIterator() {
        check(
            MapValues::iterateWithIterator,
            between(3..5),
            { map, _ -> map == null },
            { map, _ -> null in map.values },
            { map, result -> map != null && result == map.values.sum() },
            coverage = DoNotCalculate
        )
    }
}