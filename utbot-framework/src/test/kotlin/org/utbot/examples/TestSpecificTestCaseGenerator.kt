package org.utbot.examples

import kotlinx.coroutines.flow.collect
import mu.KotlinLogging
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.engine.EngineController
import org.utbot.engine.Mocker
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.util.jimpleBody
import java.nio.file.Path

/**
 * Special [UtMethodTestSet] generator for test methods that has a correct
 * wrapper for suspend function [TestCaseGenerator.generateAsync].
 */
class TestSpecificTestCaseGenerator(
    buildDir: Path,
    classpath: String?,
    dependencyPaths: String,
    engineActions: MutableList<(UtBotSymbolicEngine) -> Unit> = mutableListOf(),
    isCanceled: () -> Boolean = { false },
): TestCaseGenerator(buildDir, classpath, dependencyPaths, engineActions, isCanceled) {

    private val logger = KotlinLogging.logger {}

    fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtMethodTestSet {
        if (isCanceled()) {
            return UtMethodTestSet(method)
        }

        logger.trace { "UtSettings:${System.lineSeparator()}" + UtSettings.toString() }

        val executions = mutableListOf<UtExecution>()
        val errors = mutableMapOf<String, Int>()

        val mockAlwaysDefaults = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id }
        val defaultTimeEstimator = ExecutionTimeEstimator(UtSettings.utBotGenerationTimeoutInMillis, 1)

        val forceMockListener = ForceMockListener.create(this, conflictTriggers)
        val forceStaticMockListener = ForceStaticMockListener.create(this, conflictTriggers)

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                super
                    .generateAsync(EngineController(), method, mockStrategy, mockAlwaysDefaults, defaultTimeEstimator)
                    .collect {
                        when (it) {
                            is UtExecution -> executions += it
                            is UtError -> errors.merge(it.description, 1, Int::plus)
                        }
                    }
            }
        }

        forceMockListener.detach(this, forceMockListener)
        forceStaticMockListener.detach(this, forceStaticMockListener)

        val minimizedExecutions = super.minimizeExecutions(executions)
        return UtMethodTestSet(method, minimizedExecutions, jimpleBody(method), errors)
    }
}