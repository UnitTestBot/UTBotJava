package org.utbot.examples.collections

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

// TODO failed Kotlin compilation SAT-1332
class MapKeySetTest : UtValueTestCaseChecker(
    testClass = MapKeySet::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testRemoveFromKeySet() {
        withoutMinimization { // TODO: JIRA:1506
            checkWithException(
                MapKeySet::removeFromKeySet,
                ignoreExecutionsNumber,
                { map, _, result -> map == null && result.isException<NullPointerException>() },
                { map, i, result -> i !in map.keys && result.getOrNull() == map }, // one of these will be minimized
                { map, i, result -> // one of these will be minimized
                    val resultMap = result.getOrNull()!!
                    val mapKeysContainsI = i in map.keys
                    val mapContainsAllKeysInResult = map.keys.containsAll(resultMap.keys)
                    val resultDoesntContainI = resultMap.keys.size == map.keys.size - 1 && i !in resultMap.keys
                    mapKeysContainsI && mapContainsAllKeysInResult && resultDoesntContainI
                },
            )
        }
    }

    @Test
    fun testAddToKeySet() {
        checkWithException(
            MapKeySet::addToKeySet,
            ignoreExecutionsNumber,
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map != null && result.isException<UnsupportedOperationException>() },
            coverage = AtLeast(70) // unreachable return
        )
    }

    @Test
    fun testGetFromKeySet() {
        withoutMinimization { // TODO: JIRA:1506
            check(
                MapKeySet::getFromKeySet,
                ignoreExecutionsNumber, // branches with null keys may appear
                { map, _, _ -> map == null },
                { map, i, result -> i !in map && result == 1 }, // one of these will be minimized
                { map, i, result -> i in map && result == 1 }, // one of these will be minimized
                coverage = AtLeast(90) // 18/20 instructions, unreachable branch
            )
        }
    }

    @Test
    fun testIteratorHasNext() {
        check(
            MapKeySet::iteratorHasNext,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, result -> map.keys.isEmpty() && result == 0 },
            { map, result -> map.keys.isNotEmpty() && result == map.keys.size },
        )
    }

    @Test
    fun testIteratorNext() {
        withPushingStateFromPathSelectorForConcrete {
            checkWithException(
                MapKeySet::iteratorNext,
                ignoreExecutionsNumber,
                { map, result -> map == null && result.isException<NullPointerException>() },
                { map, result -> map.keys.isEmpty() && result.isException<NoSuchElementException>() },
                // test should work as long as default class for map is LinkedHashMap
                { map, result -> map.keys.isNotEmpty() && result.getOrNull() == map.keys.first() },
            )
        }
    }

    @Test
    fun testIteratorRemove() {
        checkWithException(
            MapKeySet::iteratorRemove,
            ignoreExecutionsNumber,
            { map, result -> map == null && result.isException<NullPointerException>() },
            { map, result -> map.keys.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, result ->
                val resultMap = result.getOrNull()!!
                val mapContainsAllKeysInResult = map.keys.isNotEmpty() && map.keys.containsAll(resultMap.keys)
                val resultDoesntContainFirstKey = resultMap.keys.size == map.keys.size - 1 && map.keys.first() !in resultMap.keys
                mapContainsAllKeysInResult && resultDoesntContainFirstKey
            },
        )
    }

    @Test
    fun testIteratorRemoveOnIndex() {
        checkWithException(
            MapKeySet::iteratorRemoveOnIndex,
            ignoreExecutionsNumber,
            { _, i, result -> i == 0 && result.isSuccess && result.getOrNull() == null },
            { map, _, result -> map == null && result.isException<NullPointerException>() },
            { map, i, result -> map != null && i < 0 && result.isException<IllegalStateException>() },
            { map, i, result -> i > map.keys.size && result.isException<NoSuchElementException>() },
            // test should work as long as default class for map is LinkedHashMap
            { map, i, result ->
                val resultMap = result.getOrNull()!!
                val iInIndexRange = i in 0..map.keys.size
                val mapContainsAllKeysInResult = map.keys.containsAll(resultMap.keys)
                val resultDoesntContainIthKey = map.keys.toList()[i - 1] !in resultMap.keys
                iInIndexRange && mapContainsAllKeysInResult && resultDoesntContainIthKey
            },
        )
    }

    @Test
    fun testIterateForEach() {
        check(
            MapKeySet::iterateForEach,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, _ -> map != null && null in map.keys },
            { map, result -> map != null && result == map.keys.sum() },
        )
    }

    @Test
    fun testIterateWithIterator() {
        check(
            MapKeySet::iterateWithIterator,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, _ -> map != null && null in map.keys },
            { map, result -> map != null && result == map.keys.sum() },
        )
    }

    @Test
    fun testNullKey() {
        check(
            MapKeySet::nullKey,
            ignoreExecutionsNumber,
            { map, _ -> map == null },
            { map, result -> map != null && null in map.keys && map[null] == result },
            { map, _ -> map != null && null !in map.keys }
        )
    }
}