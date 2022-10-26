package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.ignoreExecutionsNumber

// TODO failed Kotlin compilation SAT-1332
class MapEntrySetTest : UtValueTestCaseChecker(
    testClass = MapEntrySet::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    @Disabled("JIRA:1443")
    fun testRemoveFromEntrySet() {
        checkWithException(
            MapEntrySet::removeFromEntrySet,
            between(3..7),
            { map, _, _, result -> map == null && result.isException<NullPointerException>() },
            { map, i, j, result -> map.entries.none { it.key == i && it.value == j } && result.getOrNull() == map },
            { map, i, j, result ->
                val resultMap = result.getOrNull()!!
                val mapContainsIJ = map.entries.any { it.key == i && it.value == j }
                val mapContainsAllEntriesFromResult = map.entries.containsAll(resultMap.entries)
                val resultDoesntContainIJ =
                    resultMap.entries.size == map.entries.size - 1 && resultMap.entries.none { it.key == i && it.value == j }
                mapContainsIJ && mapContainsAllEntriesFromResult && resultDoesntContainIJ
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAddToEntrySet() {
        checkWithException(
            MapEntrySet::addToEntrySet,
            ignoreExecutionsNumber,
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map != null && result.isException<UnsupportedOperationException>() },
            coverage = AtLeast(75)
        )
    }

    @Test
    fun testGetFromEntrySet() {
        check(
            MapEntrySet::getFromEntrySet,
            ignoreExecutionsNumber,
            { map, _, _, _ -> map == null },
            { map, i, j, result -> map.none { it.key == i && it.value == j } && result == 1 },
            { map, i, j, result -> map.any { it.key == i && it.value == j } && result == 1 },
            coverage = AtLeast(94) // unreachable branch
        )
    }

    @Test
    fun testIteratorHasNext() {
        check(
            MapEntrySet::iteratorHasNext,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, result -> map.entries.isEmpty() && result == 0 },
            { map, result -> map.entries.isNotEmpty() && result == map.entries.size },
        )
    }

    @Test
    fun testIteratorNext() {
        checkWithException(
            MapEntrySet::iteratorNext,
            ignoreExecutionsNumber,
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map.entries.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, result ->
                val resultEntry = result.getOrNull()!!
                val (entryKey, entryValue) = map.entries.first()
                val (resultKey, resultValue) = resultEntry
                map.entries.isNotEmpty() && entryKey == resultKey && entryValue == resultValue
            },
        )
    }

    @Test
    fun testIteratorRemove() {
        checkWithException(
            MapEntrySet::iteratorRemove,
            ignoreExecutionsNumber,
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map.entries.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, result ->
                val resultMap = result.getOrNull()!!
                val mapContainsAllEntriesInResult = map.entries.containsAll(resultMap.entries)
                val resultDoesntContainFirstEntry =
                    resultMap.entries.size == map.entries.size - 1 && map.entries.first() !in resultMap.entries
                map.entries.isNotEmpty() && mapContainsAllEntriesInResult && resultDoesntContainFirstEntry
            },
        )
    }

    @Test
    fun testIteratorRemoveOnIndex() {
        checkWithException(
            MapEntrySet::iteratorRemoveOnIndex,
            ignoreExecutionsNumber,
            { _, i, result -> i == 0 && result.isSuccess && result.getOrNull() == null },
            { map, _, result -> map == null && result.isException<NullPointerException>() },
            { map, i, result -> map != null && i < 0 && result.isException<IllegalStateException>() },
            { map, i, result -> i > map.entries.size && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, i, result ->
                val resultMap = result.getOrNull()!!
                val iInIndexRange = i in 0..map.entries.size
                val mapContainsAllEntriesInResult = map.entries.containsAll(resultMap.entries)
                val resultDoesntContainIthEntry = map.entries.toList()[i - 1] !in resultMap.entries
                iInIndexRange && mapContainsAllEntriesInResult && resultDoesntContainIthEntry
            },
        )
    }

    @Test
    fun testIterateForEach() {
        check(
            MapEntrySet::iterateForEach,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, _ -> null in map.values },
            { map, result -> result!![0] == map.keys.sum() && result[1] == map.values.sum() },
        )
    }


    @Test
    fun testIterateWithIterator() {
        withPushingStateFromPathSelectorForConcrete {
            checkWithException(
                MapEntrySet::iterateWithIterator,
                ignoreExecutionsNumber,
                { map, result -> map == null && result.isException<NullPointerException>() },
                { map, result -> map.isEmpty() && result.getOrThrow().contentEquals(intArrayOf(0, 0)) },
                { map, result -> map.size % 2 == 1 && result.isException<NoSuchElementException>() },
                { map, result ->
                    val evenEntryHasNullKey = map.keys.indexOf(null) % 2 == 0
                    evenEntryHasNullKey && result.isException<NullPointerException>()
                },
                { map, result ->
                    val twoElementsOrMore = map.size > 1
                    val oddEntryHasNullKey = map.values.indexOf(null) % 2 == 1
                    twoElementsOrMore && oddEntryHasNullKey && result.isException<NullPointerException>()
                },
                { map, result ->
                    val mapIsNotEmptyAndSizeIsEven = map != null && map.isNotEmpty() && map.size % 2 == 0
                    val arrayResult = result.getOrThrow()
                    val evenKeysSum = map.keys.withIndex().filter { it.index % 2 == 0 }.sumOf { it.value }
                    val oddValuesSum = map.values.withIndex().filter { it.index % 2 == 0 }.sumOf { it.value }
                    mapIsNotEmptyAndSizeIsEven && arrayResult[0] == evenKeysSum && arrayResult[1] == oddValuesSum
                },
            )
        }
    }
}