package org.utbot.examples.strings

import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException

@Disabled("TODO: Fails and takes too long")
internal class GenericExamplesTest : UtValueTestCaseChecker(
    testClass = GenericExamples::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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
