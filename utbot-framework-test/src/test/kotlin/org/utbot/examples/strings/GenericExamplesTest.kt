package org.utbot.examples.strings

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Disabled("TODO: Fails and takes too long")
internal class GenericExamplesTest : UtValueTestCaseChecker(
    testClass = GenericExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testContainsOkWithIntegerType() {
        checkWithException(
            GenericExamples<Int>::containsOk,
            eq(2),
            { obj, result -> obj == null && result.isException<NullPointerException>() },
            { obj, result -> obj != null && result.isSuccess && result.getOrNull() == false }
        )
    }

    @Test
    fun testContainsOkExampleTest() {
        check(
            GenericExamples<String>::containsOkExample,
            eq(1),
            { result -> result == true }
        )
    }
}
