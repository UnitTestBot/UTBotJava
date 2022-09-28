package org.utbot.framework.plugin.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mu.KLogger
import mu.KotlinLogging
import org.utbot.common.bracket
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.common.trace
import org.utbot.engine.*
import org.utbot.engine.executeConcretely
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.disableCoroutinesDebug
import org.utbot.framework.UtSettings.enableSynthesis
import org.utbot.framework.UtSettings.utBotGenerationTimeoutInMillis
import org.utbot.framework.UtSettings.warmupConcreteExecution
import org.utbot.framework.codegen.model.util.checkFrameworkDependencies
import org.utbot.framework.concrete.UtConcreteExecutionData
import org.utbot.framework.concrete.UtExecutionInstrumentation
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.minimization.minimizeTestCase
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.synthesis.Synthesizer
import org.utbot.framework.synthesis.postcondition.constructors.EmptyPostCondition
import org.utbot.framework.synthesis.postcondition.constructors.PostConditionConstructor
import org.utbot.framework.util.SootUtils
import org.utbot.framework.util.executableId
import org.utbot.framework.util.jimpleBody
import org.utbot.framework.util.toModel
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.warmup
import org.utbot.instrumentation.warmup.Warmup
import soot.SootMethod
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.reflect.KCallable

/**
 * Generates test cases: one by one or a whole set for the method under test.
 *
 * Note: the instantiating of [TestCaseGenerator] may take some time,
 * because it requires initializing Soot for the current [buildDir] and [classpath].
 *
 * @param jdkInfo specifies the JRE and the runtime library version used for analysing system classes and user's code.
 * @param forceSootReload forces to reinitialize Soot even if the previous buildDir equals to [buildDir] and previous
 * classpath equals to [classpath]. This is the case for plugin scenario, as the source code may be modified.
 */
open class TestCaseGenerator(
    private val buildDir: Path,
    private val classpath: String?,
    private val dependencyPaths: String,
    private val jdkInfo: JdkInfo,
    val engineActions: MutableList<(UtBotSymbolicEngine) -> Unit> = mutableListOf(),
    val isCanceled: () -> Boolean = { false },
    val forceSootReload: Boolean = true,
) {
    private val logger: KLogger = KotlinLogging.logger {}
    private val timeoutLogger: KLogger = KotlinLogging.logger(logger.name + ".timeout")

    private val classpathForEngine: String
        get() = buildDir.toString() + (classpath?.let { File.pathSeparator + it } ?: "")

    init {
        if (!isCanceled()) {
            checkFrameworkDependencies(dependencyPaths)

            logger.trace("Initializing ${this.javaClass.name} with buildDir = $buildDir, classpath = $classpath")


            if (disableCoroutinesDebug) {
                System.setProperty(kotlinx.coroutines.DEBUG_PROPERTY_NAME, kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF)
            }

            timeoutLogger.trace().bracket("Soot initialization") {
                SootUtils.runSoot(buildDir, classpath, forceSootReload, jdkInfo)
            }

            //warmup
            if (warmupConcreteExecution) {
                ConcreteExecutor(
                    UtExecutionInstrumentation,
                    classpathForEngine,
                    dependencyPaths
                ).apply {
                    classLoader = utContext.classLoader
                    withUtContext(UtContext(Warmup::class.java.classLoader)) {
                        runBlocking {
                            constructExecutionsForWarmup().forEach { (method, data) ->
                                executeAsync(method, emptyArray(), data)
                            }
                        }
                    }
                    warmup()
                }
            }
        }
    }

    fun minimizeExecutions(executions: List<UtExecution>): List<UtExecution> =
        if (UtSettings.testMinimizationStrategyType == TestSelectionStrategyType.DO_NOT_MINIMIZE_STRATEGY) {
            executions
        } else {
            minimizeTestCase(executions) { it.result::class.java }
        }

    @Throws(CancellationException::class)
    fun generateAsync(
        controller: EngineController,
        method: SymbolicEngineTarget,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        executionTimeEstimator: ExecutionTimeEstimator = ExecutionTimeEstimator(utBotGenerationTimeoutInMillis, 1),
        useSynthesis: Boolean = enableSynthesis,
        postConditionConstructor: PostConditionConstructor = EmptyPostCondition,
    ): Flow<UtResult> {
        val engine = createSymbolicEngine(
            controller,
            method,
            mockStrategy,
            chosenClassesToMockAlways,
            executionTimeEstimator,
            useSynthesis,
            postConditionConstructor,
        )
        engineActions.map { engine.apply(it) }
        engineActions.clear()
        return defaultTestFlow(engine, executionTimeEstimator.userTimeout)
    }

    fun generate(
        methods: List<ExecutableId>,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        methodsGenerationTimeout: Long = utBotGenerationTimeoutInMillis,
        generate: (engine: UtBotSymbolicEngine) -> Flow<UtResult> = defaultTestFlow(methodsGenerationTimeout)
    ): List<UtMethodTestSet> {
        if (isCanceled()) return methods.map { UtMethodTestSet(it) }

        val executionStartInMillis = System.currentTimeMillis()
        val executionTimeEstimator = ExecutionTimeEstimator(methodsGenerationTimeout, methods.size)

        val currentUtContext = utContext

        val method2controller = methods.associateWith { EngineController() }
        val method2executions = methods.associateWith { mutableListOf<UtExecution>() }
        val method2errors = methods.associateWith { mutableMapOf<String, Int>() }

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                for ((method, controller) in method2controller) {
                    controller.job = launch(currentUtContext) {
                        if (!isActive) return@launch

                        //yield one to
                        yield()

                        val engine: UtBotSymbolicEngine = createSymbolicEngine(
                            controller,
                            SymbolicEngineTarget.from(method),
                            mockStrategy,
                            chosenClassesToMockAlways,
                            executionTimeEstimator,
                            enableSynthesis,
                            EmptyPostCondition
                        )

                        engineActions.map { engine.apply(it) }

                        generate(engine)
                            .catch {
                                logger.error(it) { "Error in flow" }
                            }
                            .collect {
                                when (it) {
                                    is UtExecution -> method2executions.getValue(method) += it
                                    is UtError -> method2errors.getValue(method).merge(it.description, 1, Int::plus)
                                }
                            }
                    }
                    controller.paused = true
                }

                // All jobs are in the method2controller now (paused). execute them with timeout

                GlobalScope.launch {
                    while (isActive) {
                        var activeCount = 0
                        for ((method, controller) in method2controller) {
                            if (!controller.job!!.isActive) continue
                            activeCount++

                            method2controller.values.forEach { it.paused = true }
                            controller.paused = false

                            logger.info { "|> Resuming method $method" }
                            val startTime = System.currentTimeMillis()
                            while (controller.job!!.isActive &&
                                (System.currentTimeMillis() - startTime) < executionTimeEstimator.timeslotForOneToplevelMethodTraversalInMillis
                            ) {
                                updateLifecycle(
                                    executionStartInMillis,
                                    executionTimeEstimator,
                                    method2controller.values,
                                    this
                                )
                                yield()
                            }
                        }
                        if (activeCount == 0) break
                    }
                }
            }
        }
        ConcreteExecutor.defaultPool.close() // TODO: think on appropriate way to close child processes


        return methods.map { method ->
            UtMethodTestSet(
                method,
                minimizeExecutions(method2executions.getValue(method).toAssemble(method)),
                jimpleBody(method),
                method2errors.getValue(method)
            )
        }
    }

    private fun constructExecutionsForWarmup(): Sequence<Pair<KCallable<*>, UtConcreteExecutionData>> =
        UtModelConstructor(IdentityHashMap()).run {
            sequenceOf(
                Warmup::doWarmup1 to UtConcreteExecutionData(
                    EnvironmentModels(
                        construct(Warmup(5), Warmup::class.java.id),
                        listOf(construct(Warmup(1), Warmup::class.java.id)),
                        emptyMap()
                    ), emptyList()
                ),
                Warmup::doWarmup2 to UtConcreteExecutionData(
                    EnvironmentModels(
                        construct(Warmup(1), Warmup::class.java.id),
                        listOf(construct(intArrayOf(1, 2, 3), intArrayClassId)),
                        emptyMap()
                    ), emptyList()
                ),
                Warmup::doWarmup2 to UtConcreteExecutionData(
                    EnvironmentModels(
                        construct(Warmup(1), Warmup::class.java.id),
                        listOf(construct(intArrayOf(1, 2, 3, 4, 5, 6), intArrayClassId)),
                        emptyMap()
                    ), emptyList()
                ),
            )
        }

    private fun createSymbolicEngine(
        controller: EngineController,
        method: SymbolicEngineTarget,
        mockStrategyApi: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId>,
        executionTimeEstimator: ExecutionTimeEstimator,
        useSynthesis: Boolean,
        postConditionConstructor: PostConditionConstructor = EmptyPostCondition,
    ): UtBotSymbolicEngine {
        logger.debug("Starting symbolic execution for $method  --$mockStrategyApi--")
        return UtBotSymbolicEngine(
            controller,
            method,
            classpathForEngine,
            dependencyPaths = dependencyPaths,
            mockStrategy = mockStrategyApi.toModel(),
            chosenClassesToMockAlways = chosenClassesToMockAlways,
            solverTimeoutInMillis = executionTimeEstimator.updatedSolverCheckTimeoutMillis,
            useSynthesis = useSynthesis,
            postConditionConstructor = postConditionConstructor
        )
    }

    // CONFLUENCE:The+UtBot+Java+timeouts

    class ExecutionTimeEstimator(val userTimeout: Long, methodsUnderTestNumber: Int) {
        // Cut the timeout from the user in two halves
        private val halfTimeUserExpectsToWaitInMillis = userTimeout / 2

        // If the half is too much for concrete execution, decrease the concrete timeout
        var concreteExecutionBudgetInMillis =
            min(halfTimeUserExpectsToWaitInMillis, 300L * methodsUnderTestNumber)

        // The symbolic execution time is the reminder but not longer than checkSolverTimeoutMillis times methods number
        val symbolicExecutionTimeout = userTimeout - concreteExecutionBudgetInMillis

        //Allow traverse at least one method for the symbolic execution timeout
        val timeslotForOneToplevelMethodTraversalInMillis =
            symbolicExecutionTimeout / (methodsUnderTestNumber * 2)

        // Axillary field
        private val symbolicExecutionTimePerMethod = (symbolicExecutionTimeout / methodsUnderTestNumber).toInt()

        // Now we calculate the solver timeout. Each method is supposed to get some time in worst-case scenario
        val updatedSolverCheckTimeoutMillis = if (symbolicExecutionTimePerMethod < checkSolverTimeoutMillis)
            symbolicExecutionTimePerMethod else checkSolverTimeoutMillis

        init {
            // Update the concrete execution time, if symbolic execution time is small
            // because of UtSettings.checkSolverTimeoutMillis
            concreteExecutionBudgetInMillis = userTimeout - symbolicExecutionTimeout
            require(symbolicExecutionTimeout > 10)
            require(concreteExecutionBudgetInMillis > 10)
        }
    }

    private fun updateLifecycle(
        executionStartInMillis: Long,
        executionTimeEstimator: ExecutionTimeEstimator,
        controllers: Collection<EngineController>,
        timeoutCheckerCoroutine: CoroutineScope
    ) {
        val timePassed = System.currentTimeMillis() - executionStartInMillis

        if (timePassed > executionTimeEstimator.userTimeout) {
            timeoutLogger.trace {
                "Out of concrete execution time limit (" +
                        "$timePassed  > ${executionTimeEstimator.userTimeout}" +
                        "). Cancelling coroutines"
            }
            controllers.forEach { it.job!!.cancel("Timeout") }
            timeoutCheckerCoroutine.cancel("Timeout")
        } else if (!controllers.firstOrNull()!!.executeConcretely &&
            timePassed > executionTimeEstimator.symbolicExecutionTimeout
        ) {
            timeoutLogger.trace {
                "We are out of time (" +
                        "$timePassed > ${executionTimeEstimator.symbolicExecutionTimeout}" +
                        "). Switching to the concrete execution (extra ${executionTimeEstimator.concreteExecutionBudgetInMillis} ms)"
            }
            controllers.forEach { it.executeConcretely = true }
        }
    }

    internal fun generateWithPostCondition(
        method: SootMethod,
        mockStrategy: MockStrategyApi,
        postConditionConstructor: PostConditionConstructor,
    ): List<UtExecution> {
        if (isCanceled()) return emptyList()

        val executions = mutableListOf<UtExecution>()
        val errors = mutableMapOf<String, Int>()


        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                generateAsync(
                    EngineController(),
                    SymbolicEngineTarget.from(method),
                    mockStrategy,
                    useSynthesis = false,
                    postConditionConstructor = postConditionConstructor,
                ).collect {
                    when (it) {
                        is UtExecution -> executions += it
                        is UtError -> errors.merge(it.description, 1, Int::plus)
                    }
                }
            }
        }

        val minimizedExecutions = minimizeExecutions(executions)
        return minimizedExecutions
    }

    protected fun List<UtExecution>.toAssemble(method: ExecutableId): List<UtExecution> =
        map { execution ->
            val symbolicExecution = (execution as? UtSymbolicExecution)
                ?: return@map execution

            val newBeforeState = mapEnvironmentModels(method, symbolicExecution, symbolicExecution.stateBefore) {
                it.modelsBefore
            } ?: return@map execution
            val newAfterState = getConcreteAfterState(method, newBeforeState) ?: return@map execution

            symbolicExecution.copy(
                newBeforeState,
                newAfterState,
                symbolicExecution.result,
                symbolicExecution.coverage
            )
        }

    private fun mapEnvironmentModels(
        method: ExecutableId,
        symbolicExecution: UtSymbolicExecution,
        models: EnvironmentModels,
        selector: (ConstrainedExecution) -> List<UtModel>
    ): EnvironmentModels? {
        val constrainedExecution = symbolicExecution.constrainedExecution ?: return null
        val aa = Synthesizer(this@TestCaseGenerator, method, selector(constrainedExecution))
        val synthesizedModels = aa.synthesize()

        val (synthesizedThis, synthesizedParameters) = models.thisInstance?.let {
            synthesizedModels.first() to synthesizedModels.drop(1)
        } ?: (null to synthesizedModels)
        val newThisModel = models.thisInstance?.let { synthesizedThis ?: it }
        val newParameters = models.parameters.zip(synthesizedParameters).map { it.second ?: it.first }
        return EnvironmentModels(
            newThisModel,
            newParameters,
            models.statics
        )
    }

    private fun getConcreteAfterState(method: ExecutableId, stateBefore: EnvironmentModels): EnvironmentModels? = try {
        val concreteExecutor = ConcreteExecutor(
            UtExecutionInstrumentation,
            this.classpath ?: "",
            dependencyPaths
        ).apply { this.classLoader = utContext.classLoader }
        val concreteExecutionResult = runBlocking {
            concreteExecutor.executeConcretely(
                method,
                stateBefore,
                emptyList()
            )
        }
        concreteExecutionResult.stateAfter
    } catch (e: Throwable) {
        null
    }
}


