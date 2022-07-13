package org.utbot.examples

import kotlinx.coroutines.flow.collect
import mu.KotlinLogging
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.engine.EngineController
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.util.jimpleBody
import java.nio.file.Path

/**
 * Special [UtMethodTestSet] generator for test methods that has a correct
 * wrapper for suspend function [TestCaseGenerator.generateAsync].
 */
object TestSpecificTestCaseGenerator {
    private val logger = KotlinLogging.logger {}

    fun init(buildDir: Path,
             classpath: String?,
             dependencyPaths: String,
             engineActions: MutableList<(UtBotSymbolicEngine) -> Unit> = mutableListOf(),
             isCanceled: () -> Boolean = { false },
    ) = TestCaseGenerator.init(buildDir, classpath, dependencyPaths, engineActions, isCanceled)

    fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtMethodTestSet {
        logger.trace { "UtSettings:${System.lineSeparator()}" + UtSettings.toString() }

        if (TestCaseGenerator.isCanceled()) return UtMethodTestSet(method)

        val executions = mutableListOf<UtExecution>()
        val errors = mutableMapOf<String, Int>()

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(TestCaseGenerator.isCanceled) {
                TestCaseGenerator.generateAsync(EngineController(), method, mockStrategy).collect {
                    when (it) {
                        is UtExecution -> executions += it
                        is UtError -> errors.merge(it.description, 1, Int::plus)
                    }
                }
            }
        }

        val minimizedExecutions = TestCaseGenerator.minimizeExecutions(executions)
        return UtMethodTestSet(method, minimizedExecutions, jimpleBody(method), errors)
    }
}