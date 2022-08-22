package org.utbot.framework.plugin.api

import org.utbot.common.FileUtil
import org.utbot.common.bracket
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.common.trace
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.disableCoroutinesDebug
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
import org.utbot.framework.plugin.api.util.signature
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.warmup.Warmup
import java.io.File
import java.nio.file.Path
import java.util.IdentityHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.reflect.KCallable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mu.KotlinLogging
import org.utbot.engine.*
import org.utbot.engine.selectors.strategies.ScoringStrategyBuilder
import org.utbot.engine.selectors.strategies.defaultScoringStrategy
import org.utbot.framework.UtSettings.enableSynthesis
import org.utbot.framework.synthesis.Synthesizer
import org.utbot.framework.synthesis.postcondition.constructors.EmptyPostCondition
import org.utbot.framework.synthesis.postcondition.constructors.PostConditionConstructor
import soot.Scene
import soot.SootMethod
import soot.jimple.JimpleBody
import soot.toolkits.graph.ExceptionalUnitGraph

object UtBotTestCaseGenerator : TestCaseGenerator {

    private val logger = KotlinLogging.logger {}
    private val timeoutLogger = KotlinLogging.logger(logger.name + ".timeout")

    lateinit var isCanceled: () -> Boolean

    //properties to save time on soot initialization
    private var previousBuildDir: Path? = null
    private var previousClasspath: String? = null
    private var previousTimestamp: Long? = null
    private var dependencyPaths: String = ""

    override fun init(
        buildDir: Path,
        classpath: String?,
        dependencyPaths: String,
        isCanceled: () -> Boolean
    ) {
        this.isCanceled = isCanceled
        if (isCanceled()) return

        checkFrameworkDependencies(dependencyPaths)

        logger.trace("Initializing ${this.javaClass.name} with buildDir = $buildDir, classpath = $classpath")

        //optimization: maxLastModifiedRecursivelyMillis can take time
        val timestamp = if (UtSettings.classfilesCanChange) maxLastModifiedRecursivelyMillis(buildDir, classpath) else 0

        if (buildDir == previousBuildDir && classpath == previousClasspath && timestamp == previousTimestamp) {
            logger.info { "Ignoring soot initialization because parameters are the same as on previous initialization" }
            return
        }

        if (disableCoroutinesDebug) {
            System.setProperty(kotlinx.coroutines.DEBUG_PROPERTY_NAME, kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF)
        }

        timeoutLogger.trace().bracket("Soot initialization") {
            runSoot(buildDir, classpath)
        }

        previousBuildDir = buildDir
        previousClasspath = classpath
        previousTimestamp = timestamp
        this.dependencyPaths = dependencyPaths

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

    private val classpathForEngine: String
        get() = previousBuildDir!!.toString() + (previousClasspath?.let { File.pathSeparator + it } ?: "")

    private fun maxLastModifiedRecursivelyMillis(buildDir: Path, classpath: String?): Long {
        val paths = mutableListOf<File>()
        paths += buildDir.toFile()
        if (classpath != null) {
            paths += classpath.split(File.pathSeparatorChar).map { File(it) }
        }
        return FileUtil.maxLastModifiedRecursivelyMillis(paths)
    }

    @Throws(CancellationException::class)
    fun generateAsync(
        controller: EngineController,
        sootMethod: SootMethod,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        executionTimeEstimator: ExecutionTimeEstimator = ExecutionTimeEstimator(utBotGenerationTimeoutInMillis, 1),
        useSynthesis: Boolean = false,
        postConditionConstructor: PostConditionConstructor = EmptyPostCondition,
    ): Flow<UtResult> {
        val engine = createSymbolicEngine(
            controller,
            sootMethod,
            mockStrategy,
            chosenClassesToMockAlways,
            executionTimeEstimator,
            useSynthesis,
            postConditionConstructor,
        )
        return createDefaultFlow(engine)
    }

    private fun createSymbolicEngine(
        controller: EngineController,
        sootMethod: SootMethod,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId>,
        executionTimeEstimator: ExecutionTimeEstimator,
        useSynthesis: Boolean,
        postConditionConstructor: PostConditionConstructor,
    ): UtBotSymbolicEngine {
        // TODO: create classLoader from buildDir/classpath and migrate from UtMethod to MethodId?
        logger.debug("Starting symbolic execution for $sootMethod  --$mockStrategy--")
        val graph = graph(sootMethod)

        return UtBotSymbolicEngine(
            controller,
            sootMethod,
            graph,
            mockStrategy = apiToModel(mockStrategy),
            chosenClassesToMockAlways = chosenClassesToMockAlways,
            solverTimeoutInMillis = executionTimeEstimator.updatedSolverCheckTimeoutMillis,
            useSynthesis = useSynthesis,
            postConditionConstructor = postConditionConstructor,
        )
    }

    private fun createDefaultFlow(engine: UtBotSymbolicEngine): Flow<UtResult> {
        var flow = engine.traverse()
        if (UtSettings.useFuzzing) {
            flow = flowOf(flow, engine.fuzzing()).flattenConcat()
        }
        return flow
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

    fun generateForSeveralMethods(
        methods: List<UtMethod<*>>,
        mockStrategy: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId> = Mocker.javaDefaultClasses.mapTo(mutableSetOf()) { it.id },
        methodsGenerationTimeout: Long = utBotGenerationTimeoutInMillis,
        generate: (engine: UtBotSymbolicEngine) -> Flow<UtResult> = ::createDefaultFlow
    ): List<UtTestCase> {
        if (isCanceled()) return methods.map { UtTestCase(it) }

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

                        generate(
                            createSymbolicEngine(
                                controller,
                                method.toSootMethod(),
                                mockStrategy,
                                chosenClassesToMockAlways,
                                executionTimeEstimator,
                                enableSynthesis,
                                EmptyPostCondition,
                            )
                        ).collect {
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
            UtTestCase(
                method,
                minimizeExecutions(method2executions.getValue(method).toAssemble()),
                jimpleBody(method),
                method2errors.getValue(method)
            )
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
        scoringStrategy: ScoringStrategyBuilder = defaultScoringStrategy
    ): List<UtExecution> {
        if (isCanceled()) return emptyList()

        val executions = mutableListOf<UtExecution>()
        val errors = mutableMapOf<String, Int>()


        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                generateAsync(
                    EngineController(),
                    method,
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

    override fun generate(
        method: UtMethod<*>,
        mockStrategy: MockStrategyApi,
    ): UtTestCase {
        if (enableSynthesis) {
            val executions = generateWithPostCondition(
                method.toSootMethod(), mockStrategy, EmptyPostCondition, ScoringStrategyBuilder()
            )

            return UtTestCase(
                method,
                executions.toAssemble(),
                jimpleBody(method),
            )
        } else {
            logger.trace { "UtSettings:${System.lineSeparator()}" + UtSettings.toString() }

            if (isCanceled()) return UtTestCase(method)

            val executions = mutableListOf<UtExecution>()
            val errors = mutableMapOf<String, Int>()


            runIgnoringCancellationException {
                runBlockingWithCancellationPredicate(isCanceled) {
                    generateAsync(EngineController(), method.toSootMethod(), mockStrategy).collect {
                        when (it) {
                            is UtExecution -> executions += it
                            is UtError -> errors.merge(it.description, 1, Int::plus)
                        }
                    }
                }
            }

            val minimizedExecutions = minimizeExecutions(executions)
            return UtTestCase(method, minimizedExecutions, jimpleBody(method), errors)
        }
    }


    private fun minimizeExecutions(executions: List<UtExecution>): List<UtExecution> =
        if (UtSettings.testMinimizationStrategyType == TestSelectionStrategyType.DO_NOT_MINIMIZE_STRATEGY) {
            executions
        } else {
            minimizeTestCase(executions) { it.result::class.java }
        }


    fun apiToModel(mockStrategyApi: MockStrategyApi): MockStrategy =
        when (mockStrategyApi) {
            MockStrategyApi.NO_MOCKS -> MockStrategy.NO_MOCKS
            MockStrategyApi.OTHER_PACKAGES -> MockStrategy.OTHER_PACKAGES
            MockStrategyApi.OTHER_CLASSES -> MockStrategy.OTHER_CLASSES
            else -> error("Cannot map API Mock Strategy model to Engine model: $mockStrategyApi")
        }

    private fun graph(sootMethod: SootMethod): ExceptionalUnitGraph {
        val methodBody = sootMethod.jimpleBody()
        val graph = methodBody.graph()

        logger.trace { "JIMPLE for $sootMethod:\n${methodBody}" }

        return graph
    }

    fun jimpleBody(method: UtMethod<*>): JimpleBody {
        val clazz = Scene.v().classes.single { it.name == method.clazz.java.name }
        val signature = method.callable.signature
        val sootMethod = clazz.methods.single { it.pureJavaSignature == signature }

        return sootMethod.jimpleBody()
    }


    private fun List<UtExecution>.toAssemble(): List<UtExecution> =
        map { execution ->
            val oldStateBefore = execution.stateBefore

            val constrainedExecution = execution.constrainedExecution ?: return@map execution
            val aa = Synthesizer(constrainedExecution.modelsAfter)
            val synthesizedModels = try {
                aa.synthesize() ?: return@map execution
            } catch (e: Throwable) {
                logger.debug(e) { "Failure during constraint synthesis" }
                return@map execution
            }

            val newThisModel = oldStateBefore.thisInstance?.let { synthesizedModels.first() }
            val newParameters = oldStateBefore.thisInstance?.let { synthesizedModels.drop(1) } ?: synthesizedModels

            execution.copy(
                stateBefore = EnvironmentModels(
                    newThisModel,
                    newParameters,
                    oldStateBefore.statics
                )
            )
        }
}

fun UtMethod<*>.toSootMethod(): SootMethod {
    val className = clazz.java.name
    val clazz = Scene.v().classes.singleOrNull { it.name == className }
        ?: error("No such $className found in the Scene")
    val signature = callable.signature
    val sootMethod = clazz.methods.singleOrNull { it.pureJavaSignature == signature }
        ?: error("No such $signature found")

    return sootMethod
}

fun JimpleBody.graph() = ExceptionalUnitGraph(this)

