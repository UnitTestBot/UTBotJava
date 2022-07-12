package org.utbot.examples.collections

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test

// TODO failed Kotlin compilation ($ in function names, generics) SAT-1220 SAT-1332
internal class ListWrapperReturnsVoidTest : UtValueTestCaseChecker(
    testClass = ListWrapperReturnsVoidExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testRunForEach() {
        checkWithException(
            ListWrapperReturnsVoidExample::runForEach,
            eq(4),
            { l, r -> l == null && r.isException<NullPointerException>() },
            { l, r -> l.isEmpty() && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.all { it != null } && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.any { it == null } && r.getOrThrow() > 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testSumPositiveForEach() {
        checkWithException(
            ListWrapperReturnsVoidExample::sumPositiveForEach,
            eq(5),
            { l, r -> l == null && r.isException<NullPointerException>() },
            { l, r -> l.isEmpty() && r.getOrThrow() == 0 },
            { l, r -> l.isNotEmpty() && l.any { it == null } && r.isException<NullPointerException>() },
            { l, r -> l.isNotEmpty() && l.any { it <= 0 } && r.getOrThrow() == l.filter { it > 0 }.sum() },
            { l, r -> l.isNotEmpty() && l.any { it > 0 } && r.getOrThrow() == l.filter { it > 0 }.sum() }
        )
    }
}