package org.utbot.examples.enums

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.FullWithAssumptions
import org.utbot.examples.enums.ComplexEnumExamples.Color.BLUE
import org.utbot.examples.enums.ComplexEnumExamples.Color.GREEN
import org.utbot.examples.enums.ComplexEnumExamples.Color.RED
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

class ComplexEnumExamplesTest : UtValueTestCaseChecker(
    testClass = ComplexEnumExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {

    @Test
    @Disabled
    fun testReturnColors() {
        check(
            ComplexEnumExamples::returnColors,
            ignoreExecutionsNumber,
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() }
        )
    }

    @Test
    @Disabled
    fun testCopyColors() {
        check(
            ComplexEnumExamples::copyColors,
            ignoreExecutionsNumber,
            { source, result -> source.isEmpty() && result != null && result.isEmpty() },
            { source, result -> source.isNotEmpty() && result != null && result.isNotEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Disabled
    fun testEnumToEnumMapCountValues() {
        check(
            ComplexEnumExamples::enumToEnumMapCountValues,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.values.contains(RED) && r == 0 },
            { m, r -> m.isNotEmpty() && m.values.contains(RED) && m.values.count { it == RED } == r }
        )
    }

    @Test
    @Disabled
    fun testEnumToEnumMapCountKeys() {
        check(
            ComplexEnumExamples::enumToEnumMapCountKeys,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.isNotEmpty() && !m.keys.contains(GREEN) && !m.keys.contains(BLUE) && r == 0 },
            { m, r -> m.isNotEmpty() && m.keys.intersect(setOf(BLUE, GREEN)).isNotEmpty() && m.keys.count { it == BLUE || it == GREEN } == r }
        )
    }

    @Test
    @Disabled
    fun testEnumToEnumMapCountMatches() {
        check(
            ComplexEnumExamples::enumToEnumMapCountMatches,
            ignoreExecutionsNumber,
            { m, r -> m.isEmpty() && r == 0 },
            { m, r -> m.entries.count { it.key == it.value } == r }
        )
    }

    @Test
    fun testCountEqualColors() {
        check(
            ComplexEnumExamples::countEqualColors,
            ignoreExecutionsNumber,
            { a, b, c, r -> a == b && a == c && r == 3 },
            { a, b, c, r -> a == b && b != c && r == 2 },
            { a, b, c, r -> a != b && a == c && r == 2 },
            { a, b, c, r -> a != b && b != c && a != c && r == 1 }
        )
    }

    @Test
    @Disabled
    fun testFindState() {
        check(
            ComplexEnumExamples::findState,
            ignoreExecutionsNumber,
            { c, r -> c in setOf(0, 127, 255) && r != null && r.code == c }
        )
    }
}
