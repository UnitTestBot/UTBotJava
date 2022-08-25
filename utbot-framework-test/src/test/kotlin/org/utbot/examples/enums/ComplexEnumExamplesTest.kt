package org.utbot.examples.enums

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.examples.enums.ComplexEnumExamples.Color
import org.utbot.examples.enums.ComplexEnumExamples.Color.BLUE
import org.utbot.examples.enums.ComplexEnumExamples.Color.GREEN
import org.utbot.examples.enums.ComplexEnumExamples.Color.RED
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

class ComplexEnumExamplesTest : UtValueTestCaseChecker(
    testClass = ComplexEnumExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
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
            { a, b, c, r -> setOf(a, b, c).size == 2 && r == 2 },
            { a, b, c, r -> a != b && b != c && a != c && r == 1 }
        )
    }

    @Test
    fun testCountNullColors() {
        check(
            ComplexEnumExamples::countNullColors,
            eq(3),
            { a, b, r -> a == null && b == null && r == 2 },
            { a, b, r -> (a == null) != (b == null) && r == 1 },
            { a, b, r -> a != null && b != null && r == 0 },
        )
    }

    @Test
    @Disabled("TODO: nested anonymous classes are not supported: https://github.com/UnitTestBot/UTBotJava/issues/617")
    fun testFindState() {
        check(
            ComplexEnumExamples::findState,
            ignoreExecutionsNumber,
            { c, r -> c in setOf(0, 127, 255) && r != null && r.code == c }
        )
    }

    @Test
    fun testCountValuesInArray() {
        fun Color.isCorrectlyCounted(inputs: Array<Color>, counts: Map<Color, Int>): Boolean =
            inputs.count { it == this } == (counts[this] ?: 0)

        check(
            ComplexEnumExamples::countValuesInArray,
            ignoreExecutionsNumber,
            { cs, r -> cs.isEmpty() && r != null && r.isEmpty() },
            { cs, r -> cs.toList().isEmpty() && r != null && r.isEmpty() },
            { cs, r -> cs.toList().isNotEmpty() && r != null && Color.values().all { it.isCorrectlyCounted(cs, r) } }
        )
    }

    @Test
    fun testCountRedInArray() {
        check(
            ComplexEnumExamples::countRedInArray,
            eq(3),
            { colors, result -> colors.count { it == RED } == result }
        )
    }
}
