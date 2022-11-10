package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testing.CodeGeneration
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber

// TODO failed Kotlin compilation SAT-1332
@Disabled
internal class ListsPart1Test : UtValueTestCaseChecker(
    testClass = Lists::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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