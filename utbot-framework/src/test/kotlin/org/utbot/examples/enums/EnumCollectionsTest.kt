package org.utbot.examples.enums

import org.junit.jupiter.api.Test
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.enums.EnumCollections.Color.BLUE
import org.utbot.examples.enums.EnumCollections.Color.GREEN
import org.utbot.examples.enums.EnumCollections.Color.RED
import org.utbot.examples.ge
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

class EnumCollectionsTest : UtValueTestCaseChecker(
    testClass = EnumCollections::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {

    @Test
    fun testReturnColors() {
        check(
            EnumCollections::returnColors,
            ge(2),
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCopyColors() {
        check(
            EnumCollections::copyColors,
            ge(2),
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEnumToEnumMapCountValues() {
        check(
            EnumCollections::enumToEnumMapCountValues,
            ge(3),
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.values.contains(RED) && r == 0 },
            { m, r -> m.isNotEmpty() && m.values.contains(RED) && m.values.count { it == RED } == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEnumToEnumMapCountKeys() {
        check(
            EnumCollections::enumToEnumMapCountKeys,
            ge(3),
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.keys.contains(GREEN) && !m.keys.contains(BLUE) && r == 0 },
            { m, r -> m.isNotEmpty() && m.keys.intersect(setOf(BLUE, GREEN)).isNotEmpty() && m.keys.count { it == BLUE || it == GREEN } == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEnumToEnumMapCountMatches() {
        check(
            EnumCollections::enumToEnumMapCountMatches,
            ge(2),
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.entries.count { it.key == it.value } == r },
            coverage = DoNotCalculate
        )
    }
}
