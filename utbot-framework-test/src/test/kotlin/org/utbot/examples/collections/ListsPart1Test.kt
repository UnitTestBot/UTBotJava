package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
@Disabled
internal class ListsPart1Test : UtValueTestCaseChecker(
    testClass = Lists::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testIterableContains() {
        check(
            Lists::iterableContains,
            ignoreExecutionsNumber,
            { iterable, _ -> iterable == null },
            { iterable, r -> 1 in iterable && r == true },
            { iterable, r -> 1 !in iterable && r == false },
        )
    }
}