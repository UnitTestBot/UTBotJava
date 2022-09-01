package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation ($ in names, generics) SAT-1220 SAT-1332
internal class MapsPart2Test : UtValueTestCaseChecker(
    testClass = Maps::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReplaceEntryWithValue() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                Maps::replaceEntryWithValue,
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
    }

    @Test
    fun testMerge() {
        withoutMinimization { // TODO: JIRA:1506
            checkWithException(
                Maps::merge,
                ge(5),
                { map, _, _, result -> map == null && result.isException<NullPointerException>() },
                { map, _, value, result -> map != null && value == null && result.isException<NullPointerException>() },
                { map, key, value, result ->
                    val resultMap = result.getOrNull()!!
                    val entryWasPut = resultMap.entries.all { it.key == key && it.value == value || it in map.entries }
                    key !in map && value != null && entryWasPut
                },
                { map, key, value, result ->
                    val resultMap = result.getOrNull()!!
                    val valueInMapIsNull = key in map && map[key] == null
                    val valueWasReplaced = resultMap[key] == value
                    val otherValuesWerentTouched = resultMap.entries.all { it.key == key || it in map.entries }
                    value != null && valueInMapIsNull && valueWasReplaced && otherValuesWerentTouched
                },
                { map, key, value, result ->
                    val resultMap = result.getOrNull()!!
                    val valueInMapIsNotNull = map[key] != null
                    val valueWasMerged = resultMap[key] == map[key]!! + value
                    val otherValuesWerentTouched = resultMap.entries.all { it.key == key || it in map.entries }
                    value != null && valueInMapIsNotNull && valueWasMerged && otherValuesWerentTouched
                },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testPutAllEntries() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                Maps::putAllEntries,
                ge(5),
                { map, _, _ -> map == null },
                { map, other, _ -> map != null && other == null },
                { map, other, result -> map != null && other != null && map.keys.containsAll(other.keys) && result == 0 },
                { map, other, result -> map != null && other != null && other.keys.all { it !in map.keys } && result == 1 },
                { map, other, result ->
                    val notNull = map != null && other != null
                    val mapContainsAtLeastOneKeyOfOther = other.keys.any { it in map.keys }
                    val mapDoesNotContainAllKeysOfOther = !map.keys.containsAll(other.keys)
                    notNull && mapContainsAtLeastOneKeyOfOther && mapDoesNotContainAllKeysOfOther && result == 2
                },
                coverage = DoNotCalculate
            )
        }
    }
}