package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber

// TODO failed Kotlin compilation SAT-1332
@Disabled
internal class ListsPart2Test : UtValueTestCaseChecker(
    testClass = Lists::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testCollectionContains() {
        check(
            Lists::collectionContains,
            ignoreExecutionsNumber,
            { collection, _ -> collection == null },
            { collection, r -> 1 in collection && r == true },
            { collection, r -> 1 !in collection && r == false },
        )
    }
}