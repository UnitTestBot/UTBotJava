package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.atLeast
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
class ListAlgorithmsTest : UtValueTestCaseChecker(
    testClass = ListAlgorithms::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {

    @Test
    fun testMergeLists() {
        check(
            ListAlgorithms::mergeListsInplace,
            eq(4),
            { a, b, r -> b.subList(0, b.size - 1).any { a.last() < it } && r != null && r == r.sorted() },
            { a, b, r -> (a.subList(0, a.size - 1).any { b.last() <= it } || a.any { ai -> b.any { ai < it } }) && r != null && r == r.sorted() },
            { a, b, r -> a[0] < b[0] && r != null && r == r.sorted() },
            { a, b, r -> a[0] >= b[0] && r != null && r == r.sorted() },
            coverage = atLeast(94)
        )
    }
}