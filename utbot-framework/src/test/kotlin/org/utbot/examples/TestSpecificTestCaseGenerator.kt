package org.utbot.examples

import kotlinx.coroutines.flow.collect
import mu.KotlinLogging
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.engine.EngineController
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.util.jimpleBody

object TestSpecificTestCaseGenerator {
    private val logger = KotlinLogging.logger {}

    fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtTestCase {
        logger.trace { "UtSettings:${System.lineSeparator()}" + UtSettings.toString() }

        if (TestCaseGenerator.isCanceled()) return UtTestCase(method)

        val executions = mutableListOf<UtExecution>()
        val errors = mutableMapOf<String, Int>()

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(TestCaseGenerator.isCanceled) {
                TestCaseGenerator.generateTestCasesAsync(EngineController(), method, mockStrategy).collect {
                    when (it) {
                        is UtExecution -> executions += it
                        is UtError -> errors.merge(it.description, 1, Int::plus)
                    }
                }
            }
        }

        val minimizedExecutions = TestCaseGenerator.minimizeExecutions(executions)
        return UtTestCase(method, minimizedExecutions, jimpleBody(method), errors)
    }
}