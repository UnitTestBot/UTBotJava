package org.utbot.examples.enums

import org.junit.jupiter.api.Test
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.FullWithAssumptions
import org.utbot.examples.between
import org.utbot.examples.enums.EnumCollections.Color.BLUE
import org.utbot.examples.enums.EnumCollections.Color.GREEN
import org.utbot.examples.enums.EnumCollections.Color.RED
import org.utbot.examples.ge
import org.utbot.examples.ignoreExecutionsNumber
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
            ignoreExecutionsNumber,
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() }
        )
    }

    @Test
    fun testCopyColors() {
        check(
            EnumCollections::copyColors,
            ignoreExecutionsNumber,
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testEnumToEnumMapCountValues() {
        check(
            EnumCollections::enumToEnumMapCountValues,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.values.contains(RED) && r == 0 },
            { m, r -> m.isNotEmpty() && m.values.contains(RED) && m.values.count { it == RED } == r }
        )
    }

    @Test
    fun testEnumToEnumMapCountKeys() {
        check(
            EnumCollections::enumToEnumMapCountKeys,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.keys.contains(GREEN) && !m.keys.contains(BLUE) && r == 0 },
            { m, r -> m.isNotEmpty() && m.keys.intersect(setOf(BLUE, GREEN)).isNotEmpty() && m.keys.count { it == BLUE || it == GREEN } == r }
        )
    }

    @Test
    fun testEnumToEnumMapCountMatches() {
        check(
            EnumCollections::enumToEnumMapCountMatches,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.entries.count { it.key == it.value } == r }
        )
    }
}
