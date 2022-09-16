package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation ($ in names, generics) SAT-1220 SAT-1332
internal class MapsPart1Test : UtValueTestCaseChecker(
    testClass = Maps::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testPutElementIfAbsent() {
        withoutMinimization { // TODO: JIRA:1506
            check(
                Maps::putElementIfAbsent,
                ignoreExecutionsNumber,
                { map, _, _, _ -> map == null },
                { map, key, _, result -> map != null && key in map && result == map },
                { map, key, value, result ->
                    val valueWasPut = result!![key] == value && result.size == map.size + 1
                    val otherValuesWerentTouched = result.entries.containsAll(map.entries)
                    key !in map && valueWasPut && otherValuesWerentTouched
                },
                coverage = AtLeast(90) // unreachable else branch in MUT
            )
        }
    }

    @Test
    fun testReplaceEntry() {
        check(
            Maps::replaceEntry,
            between(3..6),
            { map, _, _, _ -> map == null },
            { map, key, _, result -> key !in map && result == map },
            { map, key, value, result ->
                val valueWasReplaced = result!![key] == value
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                key in map && valueWasReplaced && otherValuesWerentTouched
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun createTest() {
        check(
            Maps::create,
            eq(5),
            { keys, _, _ -> keys == null },
            { keys, _, result -> keys.isEmpty() && result!!.isEmpty() },
            { keys, values, result -> keys.isNotEmpty() && values == null },
            { keys, values, result -> keys.isNotEmpty() && values.size < keys.size },
            { keys, values, result ->
                keys.isNotEmpty() && values.size >= keys.size &&
                        result!!.size == keys.size && keys.indices.all { result[keys[it]] == values[it] }
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testToString() {
        check(
            Maps::mapToString,
            eq(1),
            { a, b, c, r -> r == Maps().mapToString(a, b, c) }
        )
    }

    @Test
    fun testFindAllChars() {
        check(
            Maps::countChars,
            eq(3),
            { s, _ -> s == null },
            { s, result -> s == "" && result!!.isEmpty() },
            { s, result -> s != "" && result == s.groupingBy { it }.eachCount() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun putElementsTest() {
        check(
            Maps::putElements,
            ge(5),
            { map, _, _ -> map == null },
            { map, array, _ -> map != null && map.isNotEmpty() && array == null },
            { map, _, result -> map.isEmpty() && result == map },
            { map, array, result -> map.isNotEmpty() && array.isEmpty() && result == map },
            { map, array, result ->
                map.size >= 1 && array.isNotEmpty()
                        && result == map.toMutableMap().apply { putAll(array.map { it to it }) }
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun removeEntries() {
        check(
            Maps::removeElements,
            ge(6),
            { map, _, _, _ -> map == null },
            { map, i, j, res -> map != null && (i !in map || map[i] == null) && (j !in map || map[j] == null) && res == -1 },
            { map, i, j, res -> map != null && map.isNotEmpty() && i !in map && j in map && res == 4 },
            { map, i, j, res -> map != null && map.isNotEmpty() && i in map && (j !in map || j == i) && res == 3 },
            { map, i, j, res -> map != null && map.size >= 2 && i in map && j in map && i > j && res == 2 },
            { map, i, j, res -> map != null && map.size >= 2 && i in map && j in map && i < j && res == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun createWithDifferentTypeTest() {
        check(
            Maps::createWithDifferentType,
            eq(2),
            { seed, result -> seed % 2 != 0 && result is java.util.LinkedHashMap },
            { seed, result -> seed % 2 == 0 && result !is java.util.LinkedHashMap && result is java.util.HashMap },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun removeCustomObjectTest() {
        check(
            Maps::removeCustomObject,
            ge(3),
            { map, _, _ -> map == null },
            { map, i, result -> (map.isEmpty() || CustomClass(i) !in map) && result == null },
            { map, i, result -> map.isNotEmpty() && CustomClass(i) in map && result == map[CustomClass(i)] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testComputeValue() {
        check(
            Maps::computeValue,
            between(3..5),
            { map, _, _ -> map == null },
            { map, key, result ->
                val valueWasUpdated = result!![key] == key + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] == null && valueWasUpdated && otherValuesWerentTouched
            },
            { map, key, result ->
                val valueWasUpdated = result!![key] == map[key]!! + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] != null && valueWasUpdated && otherValuesWerentTouched
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testComputeValueWithMocks() {
        check(
            Maps::computeValue,
            between(3..5),
            { map, _, _ -> map == null },
            { map, key, result ->
                val valueWasUpdated = result!![key] == key + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] == null && valueWasUpdated && otherValuesWerentTouched
            },
            { map, key, result ->
                val valueWasUpdated = result!![key] == map[key]!! + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] != null && valueWasUpdated && otherValuesWerentTouched
            },
            mockStrategy = MockStrategyApi.OTHER_PACKAGES, // checks that we do not generate mocks for lambda classes
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testComputeValueIfAbsent() {
        check(
            Maps::computeValueIfAbsent,
            between(3..5),
            { map, _, _ -> map == null },
            { map, key, result -> map[key] != null && result == map },
            { map, key, result ->
                val valueWasUpdated = result!![key] == key + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] == null && valueWasUpdated && otherValuesWerentTouched
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testComputeValueIfPresent() {
        check(
            Maps::computeValueIfPresent,
            between(3..5),
            { map, _, _ -> map == null },
            { map, key, result -> map[key] == null && result == map },
            { map, key, result ->
                val valueWasUpdated = result!![key] == map[key]!! + 1
                val otherValuesWerentTouched = result.entries.all { it.key == key || it in map.entries }
                map[key] != null && valueWasUpdated && otherValuesWerentTouched
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testClearEntries() {
        check(
            Maps::clearEntries,
            between(3..4),
            { map, _ -> map == null },
            { map, result -> map.isEmpty() && result == 0 },
            { map, result -> map.isNotEmpty() && result == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testContainsKey() {
        check(
            Maps::containsKey,
            between(3..5),
            { map, _, _ -> map == null },
            { map, key, result -> key !in map && result == 0 },
            { map, key, result -> key in map && result == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testContainsValue() {
        check(
            Maps::containsValue,
            between(3..6),
            { map, _, _ -> map == null },
            { map, value, result -> value !in map.values && result == 0 },
            { map, value, result -> value in map.values && result == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetOrDefaultElement() {
        check(
            Maps::getOrDefaultElement,
            between(4..6),
            { map, _, _ -> map == null },
            { map, i, result -> i !in map && result == 1 },
            { map, i, result -> i in map && map[i] == null && result == 0 },
            { map, i, result -> i in map && map[i] != null && result == map[i] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemoveKeyWithValue() {
        check(
            Maps::removeKeyWithValue,
            ge(6),
            { map, _, _, _ -> map == null },
            { map, key, value, result -> key !in map && value !in map.values && result == 0 },
            { map, key, value, result -> key in map && value !in map.values && result == -1 },
            { map, key, value, result -> key !in map && value in map.values && result == -2 },
            { map, key, value, result -> key in map && map[key] == value && result == 3 },
            { map, key, value, result -> key in map && value in map.values && map[key] != value && result == -3 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReplaceAllEntries() {
        check(
            Maps::replaceAllEntries,
            between(5..6),
            { map, _ -> map == null },
            { map, result -> map.isEmpty() && result == null },
            { map, _ -> map.isNotEmpty() && map.containsValue(null) },
            { map, result ->
                val precondition = map.isNotEmpty() && !map.containsValue(null)
                val firstBranchInLambdaExists = map.entries.any { it.key > it.value }
                val valuesWereReplaced =
                    result == map.mapValues { if (it.key > it.value) it.value + 1 else it.value - 1 }
                precondition && firstBranchInLambdaExists && valuesWereReplaced
            },
            { map, result ->
                val precondition = map.isNotEmpty() && !map.containsValue(null)
                val secondBranchInLambdaExists = map.entries.any { it.key <= it.value }
                val valuesWereReplaced =
                    result == map.mapValues { if (it.key > it.value) it.value + 1 else it.value - 1 }
                precondition && secondBranchInLambdaExists && valuesWereReplaced
            },
            coverage = DoNotCalculate
        )
    }
}