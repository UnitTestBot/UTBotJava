package org.utbot.examples.taint.alias

import org.junit.jupiter.api.Test
import org.utbot.engine.taint.TaintAnalysisError
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.Compilation
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

class Injection550Test : UtValueTestCaseChecker(
    testClass = CWE_89_SQL_Injection_console__env_execute_550::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA, Compilation)
    )
) {
    @Test
    fun testBad() {
        checkWithException(
            CWE_89_SQL_Injection_console__env_execute_550::bad,
            ignoreExecutionsNumber,
            { r -> r.isException<TaintAnalysisError>() },
            coverage = AtLeast(95)
        )
    }
}