package org.utbot.framework.plugin.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import mu.KLogger
import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.engine.EngineController
import org.utbot.engine.Mocker
import org.utbot.engine.UsvmSymbolicEngine
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.disableCoroutinesDebug
import org.utbot.framework.UtSettings.utBotGenerationTimeoutInMillis
import org.utbot.framework.UtSettings.warmupConcreteExecution
import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.simple.SimpleApplicationContext
import org.utbot.framework.context.simple.SimpleMockerContext
import org.utbot.framework.plugin.api.utils.checkFrameworkDependencies
import org.utbot.framework.minimization.minimizeTestCase
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.framework.util.SootUtils
import org.utbot.framework.util.jimpleBody
import org.utbot.framework.util.toModel
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.instrumentation.warmup
import org.utbot.taint.TaintConfigurationProvider
import java.io.File
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

/**
 * Generates test cases: one by one or a whole set for the method under test.
 *
 * Note: the instantiating of [TestCaseGenerator] may take some time,
 * because it requires initializing Soot for the current [buildDirs] and [classpath].
 *
 * @param jdkInfo specifies the JRE and the runtime library version used for analysing system classes and user's code.
 * @param forceSootReload forces to reinitialize Soot even if the previous buildDirs equals to [buildDirs] and previous
 * classpath equals to [classpath]. This is the case for plugin scenario, as the source code may be modified.
 */
open class TestCaseGenerator(
    private val buildDirs: List<Path>,
    private val classpath: String?,
    private val dependencyPaths: String,
    private val jdkInfo: JdkInfo,
    val engineActions: MutableList<(UtBotSymbolicEngine) -> Unit> = mutableListOf(),
    val isCanceled: () -> Boolean = { false },
    val forceSootReload: Boolean = true,
    val applicationContext: ApplicationContext = SimpleApplicationContext(
        SimpleMockerContext(
            mockFrameworkInstalled = true,
            staticsMockingIsConfigured = true
        )
    ),
) {
    private val logger: KLogger = KotlinLogging.logger {}
    private val timeoutLogger: KLogger = KotlinLogging.logger(logger.name + ".timeout")
    protected val concreteExecutionContext = applicationContext.createConcreteExecutionContext(
        fullClasspath = classpathForEngine,
        classpathWithoutDependencies = buildDirs.joinToString(File.pathSeparator)
    )

    protected val classpathForEngine: String
        get() = (buildDirs + listOfNotNull(classpath)).joinToString(File.pathSeparator)

    init {
        if (!isCanceled()) {
            checkFrameworkDependencies(dependencyPaths)

            logger.trace("Initializing ${this.javaClass.name} with buildDirs = ${buildDirs.joinToString(File.pathSeparator)}, classpath = $classpath")


            if (disableCoroutinesDebug) {
                System.setProperty(kotlinx.coroutines.DEBUG_PROPERTY_NAME, kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF)
            }

            timeoutLogger.trace().measureTime({ "Soot initialization"} ) {
                SootUtils.runSoot(buildDirs, classpath, forceSootReload, jdkInfo)
            }

            //warmup
            if (warmupConcreteExecution) {
                // force pool to create an appropriate executor
                // TODO ensure that instrumented process that starts here is properly terminated
                ConcreteExecutor(
                    concreteExecutionContext.instrumentationFactory,
                    classpathForEngine,
                ).apply {
                    warmup()
                }
            }
        }
    }

    fun minimizeExecutions(
        methodUnderTest: ExecutableId,
        executions: List<UtExecution>,
        rerunExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): List<UtExecution> =
        when (UtSettings.testMinimizationStrategyType) {
            TestSelectionStrategyType.DO_NOT_MINIMIZE_STRATEGY -> executions
            TestSelectionStrategyType.COVERAGE_STRATEGY ->
                concreteExecutionContext.transformExecutionsAfterMinimization(
                    minimizeTestCase(
                        concreteExecutionContext.transformExecutionsBeforeMinimization(
                            executions,
                            methodUnderTest,
                        ),
                        executionToTestSuite = { it.result::class.java }
                    ),
                    methodUnderTest,
                    rerunExecutor = rerunExecutor,
                )
        }

    @Throws(CancellationException::class)
    fun generateAsync(
        controller: EngineController,
        method: ExecutableId,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        executionTimeEstimator: ExecutionTimeEstimator = ExecutionTimeEstimator(utBotGenerationTimeoutInMillis, 1),
        userTaintConfigurationProvider: TaintConfigurationProvider? = null,
    ): Flow<UtResult> = flow {
        if (isCanceled())
            return@flow

        val contextLoadingResult = loadConcreteExecutionContext()
        emitAll(flowOf(*contextLoadingResult.utErrors.toTypedArray()))
        if (!contextLoadingResult.contextLoaded)
            return@flow

        try {
            val engine = createSymbolicEngine(
                controller,
                method,
                mockStrategy,
                chosenClassesToMockAlways,
                applicationContext,
                executionTimeEstimator,
                userTaintConfigurationProvider,
            )
            engineActions.map { engine.apply(it) }
            engineActions.clear()
            emitAll(defaultTestFlow(engine, executionTimeEstimator.userTimeout))
        } catch (e: Exception) {
            logger.error(e) {"Generate async failed"}
            throw e
        }
    }

    fun generate(
        methods: List<ExecutableId>,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        utBotTimeout: Long = utBotGenerationTimeoutInMillis,
        userTaintConfigurationProvider: TaintConfigurationProvider? = null,
        generate: (engine: UtBotSymbolicEngine) -> Flow<UtResult> = defaultTestFlow(utBotTimeout),
        usvmTimeoutMillis: Long = 0,
    ): List<UtMethodTestSet> = ConcreteExecutor.defaultPool.use { _ -> // TODO: think on appropriate way to close instrumented processes
        if (isCanceled()) return@use methods.map { UtMethodTestSet(it) }

        val contextLoadingResult = loadConcreteExecutionContext()

        val method2errors: Map<ExecutableId, MutableMap<String, Int>> = methods.associateWith {
            contextLoadingResult.utErrors.associateTo(mutableMapOf()) { it.description to 1 }
        }

        if (!contextLoadingResult.contextLoaded)
            return@use methods.map { method -> UtMethodTestSet(method, errors = method2errors.getValue(method)) }

        val executionStartInMillis = System.currentTimeMillis()
        val executionTimeEstimator = ExecutionTimeEstimator(utBotTimeout, methods.size)

        val currentUtContext = utContext

        val method2controller = methods.associateWith { EngineController() }
        val method2executions = methods.associateWith { mutableListOf<UtExecution>() }

        val conflictTriggers = ConflictTriggers()
        val forceMockListener = ForceMockListener.create(this, conflictTriggers)
        val forceStaticMockListener = ForceStaticMockListener.create(this, conflictTriggers)

        suspend fun consumeUtResultFlow(utResultFlow: Flow<Pair<ExecutableId, UtResult>>) =
            utResultFlow.catch {
                logger.error(it) { "Error in flow" }
            }
                .collect { (executableId, utResult) ->
                    when (utResult) {
                        is UtExecution -> {
                            if (utResult is UtSymbolicExecution &&
                                (conflictTriggers.triggered(Conflict.ForceMockHappened) ||
                                        conflictTriggers.triggered(Conflict.ForceStaticMockHappened))
                            ) {
                                utResult.containsMocking = true
                            }
                            method2executions.getValue(executableId) += utResult
                        }

                        is UtError -> {
                            method2errors.getValue(executableId).merge(utResult.description, 1, Int::plus)
                            logger.error(utResult.error) { "UtError occurred" }
                        }
                    }
                }

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                for ((method, controller) in method2controller) {
                    controller.job = launch(currentUtContext) {
                        if (!isActive) return@launch

                        try {
                            //yield one to
                            yield()

                            val engine: UtBotSymbolicEngine = createSymbolicEngine(
                                controller,
                                method,
                                mockStrategy,
                                chosenClassesToMockAlways,
                                applicationContext,
                                executionTimeEstimator,
                                userTaintConfigurationProvider,
                            )

                            engineActions.map { engine.apply(it) }
                            engineActions.clear()

                            consumeUtResultFlow(generate(engine).map { utResult -> method to utResult })
                        } catch (e: Exception) {
                            logger.error(e) {"Error in engine"}
                            throw e
                        }
                    }
                    controller.paused = true
                    conflictTriggers.reset(Conflict.ForceMockHappened, Conflict.ForceStaticMockHappened)
                }

                // All jobs are in the method2controller now (paused). execute them with timeout

                GlobalScope.launch {
                    logger.debug("test generator global scope lifecycle check started")
                    while (isActive) {
                        var activeCount = 0
                        for ((method, controller) in method2controller) {
                            if (!controller.job!!.isActive) continue
                            activeCount++

                            method2controller.values.forEach { it.paused = true }
                            controller.paused = false

                            logger.info { "Resuming method $method" }
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
                    logger.debug("test generator global scope lifecycle check ended")
                }

                consumeUtResultFlow(UsvmSymbolicEngine.runUsvmGeneration(methods, classpathForEngine, usvmTimeoutMillis))
            }
        }

        forceMockListener.detach(this, forceMockListener)
        forceStaticMockListener.detach(this, forceStaticMockListener)

        return@use methods.map { method ->
            UtMethodTestSet(
                method,
                minimizeExecutions(
                    method,
                    method2executions.getValue(method),
                    rerunExecutor = ConcreteExecutor(concreteExecutionContext.instrumentationFactory, classpathForEngine)
                ),
                jimpleBody(method),
                method2errors.getValue(method)
            )
        }
    }

    private fun createSymbolicEngine(
        controller: EngineController,
        method: ExecutableId,
        mockStrategyApi: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId>,
        applicationContext: ApplicationContext,
        executionTimeEstimator: ExecutionTimeEstimator,
        userTaintConfigurationProvider: TaintConfigurationProvider? = null,
    ): UtBotSymbolicEngine {
        logger.debug("Starting symbolic execution for $method  --$mockStrategyApi--")
        return UtBotSymbolicEngine(
            controller,
            method,
            classpathForEngine,
            dependencyPaths = dependencyPaths,
            mockStrategy = mockStrategyApi.toModel(),
            chosenClassesToMockAlways = chosenClassesToMockAlways,
            applicationContext = applicationContext,
            concreteExecutionContext = concreteExecutionContext,
            solverTimeoutInMillis = executionTimeEstimator.updatedSolverCheckTimeoutMillis,
            userTaintConfigurationProvider = userTaintConfigurationProvider,
        )
    }

    // CONFLUENCE:The+UtBot+Java+timeouts

    class ExecutionTimeEstimator(val userTimeout: Long, methodsUnderTestNumber: Int) {
        // Cut the timeout from the user in two halves
        private val halfTimeUserExpectsToWaitInMillis = userTimeout / 2

        // If the half is too much for concrete execution, decrease the concrete timeout
        val concreteExecutionBudgetInMillis = min(halfTimeUserExpectsToWaitInMillis, 300L * methodsUnderTestNumber)

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

    private fun loadConcreteExecutionContext(): ConcreteContextLoadingResult {
        // force pool to create an appropriate executor
        val concreteExecutor = ConcreteExecutor(concreteExecutionContext.instrumentationFactory, classpathForEngine)
        return concreteExecutionContext.loadContext(concreteExecutor)
    }
}
