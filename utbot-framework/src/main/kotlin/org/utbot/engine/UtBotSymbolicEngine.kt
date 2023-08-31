package org.utbot.engine

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.FeatureProcessor
import org.utbot.analytics.Predictors
import org.utbot.api.exception.UtMockAssumptionViolatedException
import org.utbot.common.debug
import org.utbot.common.measureTime
import org.utbot.engine.MockStrategy.NO_MOCKS
import org.utbot.engine.pc.*
import org.utbot.engine.selectors.*
import org.utbot.engine.selectors.nurs.NonUniformRandomSearch
import org.utbot.engine.selectors.strategies.GraphViz
import org.utbot.engine.state.ExecutionStackElement
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.state.StateLabel
import org.utbot.engine.symbolic.SymbolicState
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.types.TypeRegistry
import org.utbot.engine.types.TypeResolver
import org.utbot.engine.util.mockListeners.MockListener
import org.utbot.engine.util.mockListeners.MockListenerController
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.enableFeatureProcess
import org.utbot.framework.UtSettings.pathSelectorStepsLimit
import org.utbot.framework.UtSettings.pathSelectorType
import org.utbot.framework.UtSettings.processUnknownStatesDuringConcreteExecution
import org.utbot.framework.UtSettings.useDebugVisualization
import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.calculateSize
import org.utbot.framework.util.convertToAssemble
import org.utbot.framework.util.graph
import org.utbot.framework.util.sootMethod
import org.utbot.fuzzer.*
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.taint.*
import org.utbot.taint.model.TaintConfiguration
import soot.jimple.Stmt
import soot.tagkit.ParamNamesTag
import java.lang.reflect.Method
import java.util.function.Consumer
import kotlin.math.min
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}
val pathLogger = KotlinLogging.logger(logger.name + ".path")

//in future we should put all timeouts here
class EngineController {
    var paused: Boolean = false
    var executeConcretely: Boolean = false
    var stop: Boolean = false
    var job: Job? = null
}

//for debugging purpose only
private var stateSelectedCount = 0

private val defaultIdGenerator = ReferencePreservingIntIdGenerator()

private fun pathSelector(graph: InterProceduralUnitGraph, typeRegistry: TypeRegistry) =
    when (pathSelectorType) {
        PathSelectorType.COVERED_NEW_SELECTOR -> coveredNewSelector(graph) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.INHERITORS_SELECTOR -> inheritorsSelector(graph, typeRegistry) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.BFS_SELECTOR -> bfsSelector(graph, StrategyOption.VISIT_COUNTING) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.SUBPATH_GUIDED_SELECTOR -> subpathGuidedSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.CPI_SELECTOR -> cpInstSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.FORK_DEPTH_SELECTOR -> forkDepthSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.ML_SELECTOR -> mlSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.TORCH_SELECTOR -> mlSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.RANDOM_SELECTOR -> randomSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.RANDOM_PATH_SELECTOR -> randomPathSelector(graph, StrategyOption.DISTANCE) {
            withStepsLimit(pathSelectorStepsLimit)
        }
    }

class UtBotSymbolicEngine(
    private val controller: EngineController,
    private val methodUnderTest: ExecutableId,
    classpath: String,
    dependencyPaths: String,
    val mockStrategy: MockStrategy = NO_MOCKS,
    chosenClassesToMockAlways: Set<ClassId>,
    val applicationContext: ApplicationContext,
    val concreteExecutionContext: ConcreteExecutionContext,
    userTaintConfigurationProvider: TaintConfigurationProvider? = null,
    private val solverTimeoutInMillis: Int = checkSolverTimeoutMillis,
) : UtContextInitializer() {
    
    private val graph = methodUnderTest.sootMethod.jimpleBody().apply {
        logger.trace { "JIMPLE for $methodUnderTest:\n$this" }
    }.graph()

    private val methodUnderAnalysisStmts: Set<Stmt> = graph.stmts.toSet()
    private val globalGraph = InterProceduralUnitGraph(graph)
    private val typeRegistry: TypeRegistry = TypeRegistry()
    private val pathSelector: PathSelector = pathSelector(globalGraph, typeRegistry)

    internal val hierarchy: Hierarchy = Hierarchy(typeRegistry)

    // TODO HACK violation of encapsulation
    internal val typeResolver: TypeResolver = TypeResolver(typeRegistry, hierarchy)

    private val classUnderTest: ClassId = methodUnderTest.classId

    private val mocker: Mocker = Mocker(
        mockStrategy,
        classUnderTest,
        hierarchy,
        chosenClassesToMockAlways,
        MockListenerController(controller),
        mockerContext = applicationContext.mockerContext,
    )

    private val stateListeners: MutableList<ExecutionStateListener> = mutableListOf();

    fun addListener(listener: ExecutionStateListener): UtBotSymbolicEngine {
        stateListeners += listener
        return this
    }

    fun removeListener(listener: ExecutionStateListener): UtBotSymbolicEngine {
        stateListeners -= listener
        return this
    }

    fun attachMockListener(mockListener: MockListener) = mocker.mockListenerController?.attach(mockListener)

    fun detachMockListener(mockListener: MockListener) = mocker.mockListenerController?.detach(mockListener)

    private val statesForConcreteExecution: MutableList<ExecutionState> = mutableListOf()

    private val taintConfigurationProvider = if (UtSettings.useTaintAnalysis) {
        TaintConfigurationProviderCombiner(
            listOf(
                userTaintConfigurationProvider ?: TaintConfigurationProviderEmpty,
                TaintConfigurationProviderCached("resources", TaintConfigurationProviderResources())
            )
        )
    } else {
        TaintConfigurationProviderEmpty
    }
    private val taintConfiguration: TaintConfiguration = run {
        val config = taintConfigurationProvider.getConfiguration()
        logger.debug { "Taint analysis configuration: $config" }
        config
    }

    private val taintMarkRegistry: TaintMarkRegistry = TaintMarkRegistry()
    private val taintMarkManager: TaintMarkManager = TaintMarkManager(taintMarkRegistry)
    private val taintContext: TaintContext = TaintContext(taintMarkManager, taintConfiguration)

    private val traverser = Traverser(
        methodUnderTest,
        typeRegistry,
        hierarchy,
        typeResolver,
        globalGraph,
        mocker,
        applicationContext.typeReplacer,
        applicationContext.nonNullSpeculator,
        taintContext,
    )

    //HACK (long strings)
    internal var softMaxArraySize = 40

    private val concreteExecutor =
        ConcreteExecutor(
            concreteExecutionContext.instrumentationFactory,
            classpath,
        ).apply { this.classLoader = utContext.classLoader }

    private val featureProcessor: FeatureProcessor? =
        if (enableFeatureProcess) EngineAnalyticsContext.featureProcessorFactory(globalGraph) else null


    private val trackableResources: MutableSet<AutoCloseable> = mutableSetOf()

    private fun postTraverse() {
        for (r in trackableResources)
            try {
                r.close()
            } catch (e: Throwable) {
                logger.error(e) { "Closing resource failed" }
            }
        trackableResources.clear()
        featureProcessor?.dumpFeatures()
    }

    private suspend fun preTraverse() {
        //fixes leak in useless Context() created in AutoCloseable()
        close()
        if (!currentCoroutineContext().isActive) return
        stateSelectedCount = 0
    }

    fun traverse(): Flow<UtResult> = traverseImpl()
        .onStart { preTraverse() }
        .onCompletion { postTraverse() }

    /**
     * Traverse through all states and get results.
     *
     * This method is supposed to used when calling [traverse] is not suitable,
     * e.g. from Java programs. It runs traversing with blocking style using callback
     * to provide [UtResult].
     */
    @JvmOverloads
    fun traverseAll(consumer: Consumer<UtResult> = Consumer { }) {
        runBlocking {
            traverse().collect {
                consumer.accept(it)
            }
        }
    }

    private fun traverseImpl(): Flow<UtResult> = flow {

        require(trackableResources.isEmpty())

        if (useDebugVisualization) GraphViz(globalGraph, pathSelector)

        val initStmt = graph.head
        val initState = ExecutionState(
            initStmt,
            SymbolicState(UtSolver(typeRegistry, trackableResources, solverTimeoutInMillis)),
            executionStack = persistentListOf(ExecutionStackElement(null, method = graph.body.method))
        )

        pathSelector.offer(initState)

        pathSelector.use {

            while (currentCoroutineContext().isActive) {
                if (controller.stop)
                    break

                if (controller.paused) {
                    try {
                        yield()
                    } catch (e: CancellationException) { //todo in future we should just throw cancellation
                        break
                    }
                    continue
                }

                stateSelectedCount++
                pathLogger.trace {
                    "traverse<$methodUnderTest>: choosing next state($stateSelectedCount), " +
                            "queue size=${(pathSelector as? NonUniformRandomSearch)?.size ?: -1}"
                }

                if (UtSettings.useConcreteExecution && (controller.executeConcretely || statesForConcreteExecution.isNotEmpty())) {
                    val state = pathSelector.pollUntilFastSAT()
                        ?: statesForConcreteExecution.pollUntilSat(processUnknownStatesDuringConcreteExecution)
                        ?: break
                    // This state can contain inconsistent wrappers - for example, Map with keys but missing values.
                    // We cannot use withWrapperConsistencyChecks here because it needs solver to work.
                    // So, we have to process such cases accurately in wrappers resolving.

                    logger.trace { "executing $state concretely..." }


                    logger.debug().measureTime({ "concolicStrategy<$methodUnderTest>: execute concretely"} ) {
                        val resolver = Resolver(
                            hierarchy,
                            state.memory,
                            typeRegistry,
                            typeResolver,
                            state.solver.lastStatus as UtSolverStatusSAT,
                            methodUnderTest,
                            softMaxArraySize,
                            traverser.objectCounter
                        )

                        val resolvedParameters = state.methodUnderTestParameters
                        val (modelsBefore, _, instrumentation) = resolver.resolveModels(resolvedParameters)
                        val stateBefore = modelsBefore.constructStateForMethod(methodUnderTest)

                        try {
                            val concreteExecutionResult =
                                concreteExecutor.executeConcretely(methodUnderTest, stateBefore, instrumentation, UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis)

                            if (failureCanBeProcessedGracefully(concreteExecutionResult, executionToRollbackOn = null)) {
                                return@measureTime
                            }

                            if (concreteExecutionResult.violatesUtMockAssumption()) {
                                logger.debug { "Generated test case violates the UtMock assumption: $concreteExecutionResult" }
                                return@measureTime
                            }

                            val concreteUtExecution = UtSymbolicExecution(
                                stateBefore,
                                concreteExecutionResult.stateAfter,
                                concreteExecutionResult.result,
                                concreteExecutionResult.newInstrumentation ?: instrumentation,
                                mutableListOf(),
                                listOf(),
                                concreteExecutionResult.coverage
                            )
                            emit(concreteUtExecution)

                            logger.debug { "concolicStrategy<${methodUnderTest}>: returned $concreteUtExecution" }
                        } catch (e: CancellationException) {
                            logger.debug(e) { "Cancellation happened" }
                        } catch (e: InstrumentedProcessDeathException) {
                            emitFailedConcreteExecutionResult(stateBefore, e)
                        } catch (e: Throwable) {
                            emit(UtError("Concrete execution failed", e))
                        }
                    }

                    // I am not sure this part works correctly when concrete execution is enabled.
                    // todo test this part more accurate
                    try {
                        fireExecutionStateEvent(state)
                    } catch (ce: CancellationException) {
                        break
                    }

                } else {
                    val state = pathSelector.poll()

                    // state is null in case states queue is empty
                    // or path selector exceed some limits (steps limit, for example)
                    if (state == null) {
                        // check do we have remaining states that we can execute concretely
                        val pathSelectorStatesForConcreteExecution = pathSelector
                            .remainingStatesForConcreteExecution
                            .map { it.withWrapperConsistencyChecks() }
                        if (pathSelectorStatesForConcreteExecution.isNotEmpty()) {
                            statesForConcreteExecution += pathSelectorStatesForConcreteExecution
                            logger.debug {
                                "${pathSelectorStatesForConcreteExecution.size} remaining states " +
                                        "were moved from path selector for concrete execution"
                            }
                            continue // the next step in while loop processes concrete states
                        } else {
                            break
                        }
                    }

                    state.executingTime += measureTimeMillis {
                        val newStates = try {
                            traverser.traverse(state)
                        } catch (ex: Throwable) {
                            emit(UtError(ex.description, ex))
                            return@measureTimeMillis
                        }
                        for (newState in newStates) {
                            when (newState.label) {
                                StateLabel.INTERMEDIATE -> pathSelector.offer(newState)
                                StateLabel.CONCRETE -> statesForConcreteExecution.add(newState)
                                StateLabel.TERMINAL -> consumeTerminalState(newState)
                            }
                        }

                        // Here job can be cancelled from within traverse, e.g. by using force mocking without Mockito.
                        // So we need to make it throw CancelledException by method below:
                        currentCoroutineContext().job.ensureActive()
                    }

                    // TODO: think about concise modifying globalGraph in Traverser and UtBotSymbolicEngine
                    globalGraph.visitNode(state)

                    try {
                        fireExecutionStateEvent(state)
                    } catch (ce: CancellationException) {
                        break
                    }
                }
            }
        }
    }

    private fun fireExecutionStateEvent(state: ExecutionState) {
        stateListeners.forEach { l ->
            try {
                l.visit(globalGraph, state)
            } catch (t: Throwable) {
                logger.error(t) { "$l failed with error" }
            }
        }
    }


    /**
     * Run fuzzing flow.
     *
     * @param until is used by fuzzer to cancel all tasks if the current time is over this value
     * @param transform provides model values for a method
     */
    fun fuzzing(until: Long = Long.MAX_VALUE, transform: (JavaValueProvider) -> JavaValueProvider = { it }) = flow {
        val isFuzzable = methodUnderTest.parameters.all { classId ->
            classId != Method::class.java.id && // causes the instrumented process crash at invocation
                classId != Class::class.java.id  // causes java.lang.IllegalAccessException: java.lang.Class at sun.misc.Unsafe.allocateInstance(Native Method)
        }
        if (!isFuzzable) {
            return@flow
        }
        val errorStackTraceTracker = Trie(StackTraceElement::toString)
        var attempts = 0
        val attemptsLimit = UtSettings.fuzzingMaxAttempts
        val names = graph.body.method.tags.filterIsInstance<ParamNamesTag>().firstOrNull()?.names ?: emptyList()
        var testEmittedByFuzzer = 0

        val valueProviders = try {
            concreteExecutionContext.tryCreateValueProvider(concreteExecutor, classUnderTest, defaultIdGenerator)
        } catch (e: Exception) {
            emit(UtError(e.message ?: "Failed to create ValueProvider", e))
            return@flow
        }.let(transform)

        val coverageToMinStateBeforeSize = mutableMapOf<Trie.Node<Instruction>, Int>()

        runJavaFuzzing(
            defaultIdGenerator,
            methodUnderTest,
            constants = collectConstantsForFuzzer(graph),
            names = names,
            providers = listOf(valueProviders),
        ) { thisInstance, descr, values ->
            val diff = until - System.currentTimeMillis()
            val thresholdMillisForFuzzingOperation = 0 // may be better use 10-20 millis as it might not be possible
            // to concretely execute that values because request to instrumentation process involves
            // 1. serializing/deserializing it with kryo
            // 2. sending over rd
            // 3. concrete execution itself
            // 4. analyzing concrete result
            if (controller.job?.isActive == false || diff <= thresholdMillisForFuzzingOperation) {
                logger.info { "Fuzzing overtime: $methodUnderTest" }
                logger.info { "Test created by fuzzer: $testEmittedByFuzzer" }
                return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.STOP)
            }

            if (thisInstance?.model is UtNullModel) {
                // We should not try to run concretely any models with null-this.
                // But fuzzer does generate such values, because it can fail to generate any "good" values.
                return@runJavaFuzzing JavaFeedback(Trie.emptyNode(), Control.PASS)
            }

            val stateBefore = concreteExecutionContext.createStateBefore(
                thisInstance = thisInstance?.model,
                parameters = values.map { it.model },
                statics = emptyMap(),
                executableToCall = methodUnderTest,
                idGenerator = defaultIdGenerator
            )

            val concreteExecutionResult: UtConcreteExecutionResult? = try {
                val timeoutMillis = min(UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis, diff)
                concreteExecutor.executeConcretely(methodUnderTest, stateBefore, listOf(), timeoutMillis)
            } catch (e: CancellationException) {
                logger.debug { "Cancelled by timeout" }; null
            } catch (e: InstrumentedProcessDeathException) {
                emitFailedConcreteExecutionResult(stateBefore, e); null
            } catch (e: Throwable) {
                emit(UtError("Default concrete execution failed", e)); null
            }

            // in case an exception occurred from the concrete execution
            concreteExecutionResult ?: return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.PASS)

            // in case of processed failure in the concrete execution
            concreteExecutionResult.processedFailure()?.let { failure ->
                logger.debug { "Instrumented process failed with exception ${failure.exception} before concrete execution started" }
                return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.PASS)
            }

            if (concreteExecutionResult.violatesUtMockAssumption()) {
                logger.debug { "Generated test case by fuzzer violates the UtMock assumption: $concreteExecutionResult" }
                return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.PASS)
            }

            val result = concreteExecutionResult.result
            val coveredInstructions = concreteExecutionResult.coverage.coveredInstructions
            var trieNode: Trie.Node<Instruction>? = null

            if (coveredInstructions.isNotEmpty()) {
                trieNode = descr.tracer.add( coveredInstructions )

                val earlierStateBeforeSize = coverageToMinStateBeforeSize[trieNode]
                val curStateBeforeSize = stateBefore.calculateSize()

                if (earlierStateBeforeSize == null || curStateBeforeSize < earlierStateBeforeSize)
                    coverageToMinStateBeforeSize[trieNode] = curStateBeforeSize
                else {
                    if (++attempts >= attemptsLimit) {
                        return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.STOP)
                    }
                    return@runJavaFuzzing JavaFeedback(result = trieNode, control = Control.CONTINUE)
                }
            } else {
                logger.error { "Coverage is empty for $methodUnderTest with $values" }
                if (result is UtSandboxFailure) {
                    val stackTraceElements = result.exception.stackTrace.reversed()
                    if (errorStackTraceTracker.add(stackTraceElements).count > 1) {
                        return@runJavaFuzzing JavaFeedback(result = Trie.emptyNode(), control = Control.PASS)
                    }
                }
            }

            emit(
                UtFuzzedExecution(
                    stateBefore = stateBefore,
                    stateAfter = concreteExecutionResult.stateAfter,
                    result = concreteExecutionResult.result,
                    coverage = concreteExecutionResult.coverage,
                    fuzzingValues = values,
                    fuzzedMethodDescription = descr.description,
                    instrumentation = concreteExecutionResult.newInstrumentation ?: emptyList()
                )
            )

            testEmittedByFuzzer++
            JavaFeedback(result = trieNode ?: Trie.emptyNode(), control = Control.CONTINUE)
        }
    }

    private suspend fun FlowCollector<UtResult>.emitFailedConcreteExecutionResult(
        stateBefore: EnvironmentModels,
        e: Throwable
    ) {
        val failedConcreteExecution = UtFailedExecution(
            stateBefore = stateBefore,
            result = UtConcreteExecutionFailure(e)
        )

        emit(failedConcreteExecution)
    }

    private suspend fun FlowCollector<UtResult>.consumeTerminalState(
        state: ExecutionState,
    ) {
        // some checks to be sure the state is correct
        require(state.label == StateLabel.TERMINAL) { "Can't process non-terminal state!" }
        require(!state.isInNestedMethod()) { "The state has to correspond to the MUT" }

        val memory = state.memory
        val solver = state.solver
        val parameters = state.parameters.map { it.value }
        val symbolicResult = requireNotNull(state.methodResult?.symbolicResult) { "The state must have symbolicResult" }
        val holder = if (UtSettings.disableUnsatChecking) {
            (solver.lastStatus as? UtSolverStatusSAT) ?: return
        } else {
            requireNotNull(solver.lastStatus as? UtSolverStatusSAT) { "The state must be SAT!" }
        }

        val predictedTestName = Predictors.testName.predict(state.path)
        Predictors.testName.provide(state.path, predictedTestName, "")

        // resolving
        val resolver = Resolver(
            hierarchy,
            memory,
            typeRegistry,
            typeResolver,
            holder,
            methodUnderTest,
            softMaxArraySize,
            traverser.objectCounter
        )

        val (modelsBefore, modelsAfter, instrumentation) = resolver.resolveModels(parameters)

        val symbolicExecutionResult = resolver.resolveResult(symbolicResult)

        val stateBefore = modelsBefore.constructStateForMethod(methodUnderTest)
        val stateAfter = modelsAfter.constructStateForMethod(methodUnderTest)
        require(stateBefore.parameters.size == stateAfter.parameters.size)

        val symbolicUtExecution = UtSymbolicExecution(
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            result = symbolicExecutionResult,
            instrumentation = instrumentation,
            path = entryMethodPath(state),
            fullPath = state.fullPath(),
            symbolicSteps = getSymbolicPath(state, symbolicResult)
        )

        globalGraph.traversed(state)

        if (!UtSettings.useConcreteExecution ||
            // Can't execute concretely because overflows do not cause actual exceptions.
            // Still, we need overflows to act as implicit exceptions.
            (UtSettings.treatOverflowAsError && symbolicExecutionResult is UtOverflowFailure) ||
            // the same for taint analysis errors
            (UtSettings.useTaintAnalysis && symbolicExecutionResult is UtTaintAnalysisFailure)
        ) {
            logger.debug {
                "processResult<${methodUnderTest}>: no concrete execution allowed, " +
                        "emit purely symbolic result $symbolicUtExecution"
            }
            emit(symbolicUtExecution)
            return
        }

        // Check for lambda result as it cannot be emitted by concrete execution
        (symbolicExecutionResult as? UtExecutionSuccess)?.takeIf { it.model is UtLambdaModel }?.run {
            logger.debug {
                "processResult<${methodUnderTest}>: impossible to create concrete value for lambda result ($model), " +
                        "emit purely symbolic result $symbolicUtExecution"
            }

            emit(symbolicUtExecution)
            return
        }

        if (checkStaticMethodsMock(symbolicUtExecution)) {
            logger.debug {
                buildString {
                    append("processResult<${methodUnderTest}>: library static methods mock found ")
                    append("(we do not support it in concrete execution yet), ")
                    append("emit purely symbolic result $symbolicUtExecution")
                }
            }

            emit(symbolicUtExecution)
            return
        }


        //It's possible that symbolic and concrete stateAfter/results are diverged.
        //So we trust concrete results more.
        try {
            logger.debug().measureTime({ "processResult<$methodUnderTest>: concrete execution" } ) {

                //this can throw CancellationException
                val concreteExecutionResult = concreteExecutor.executeConcretely(
                    methodUnderTest,
                    stateBefore,
                    instrumentation,
                    UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis
                )

                if (failureCanBeProcessedGracefully(concreteExecutionResult, symbolicUtExecution)) {
                    return
                }

                if (concreteExecutionResult.violatesUtMockAssumption()) {
                    logger.debug { "Generated test case violates the UtMock assumption: $concreteExecutionResult" }
                    return
                }

                val concolicUtExecution = symbolicUtExecution.copy(
                    stateAfter = concreteExecutionResult.stateAfter,
                    result = concreteExecutionResult.result,
                    coverage = concreteExecutionResult.coverage,
                    instrumentation = concreteExecutionResult.newInstrumentation ?: instrumentation
                )

                emit(concolicUtExecution)
                logger.debug { "processResult<${methodUnderTest}>: returned $concolicUtExecution" }
            }
        } catch (e: InstrumentedProcessDeathException) {
            emitFailedConcreteExecutionResult(stateBefore, e)
        } catch (e: CancellationException) {
            logger.debug(e) { "Cancellation happened" }
        } catch (e: Throwable) {
            emit(UtError("Default concrete execution failed", e))
        }
    }

    private suspend fun FlowCollector<UtResult>.failureCanBeProcessedGracefully(
        concreteExecutionResult: UtConcreteExecutionResult,
        executionToRollbackOn: UtExecution?,
    ): Boolean {
        concreteExecutionResult.processedFailure()?.let { failure ->
            // If concrete execution failed to some reasons that are not process death or cancellation
            // when we call something that is processed successfully by symbolic engine,
            // we should:
            // - roll back to symbolic execution data ignoring failing concrete (is symbolic execution exists);
            // - do not emit an execution if there is nothing to roll back on.

            // Note that this situation is suspicious anyway, so we log a WARN message about the failure.
            executionToRollbackOn?.let {
                emit(it)
            }

            logger.warn { "Instrumented process failed with exception ${failure.exception} before concrete execution started" }
            return true
        }

        return false
    }

    /**
     * Collects entry method statement path for ML. Eliminates duplicated statements, e.g. assignment with invocation
     * in right part.
     */
    private fun entryMethodPath(state: ExecutionState): MutableList<Step> {
        val entryPath = mutableListOf<Step>()
        state.fullPath().forEach { step ->
            // TODO: replace step.stmt in methodUnderAnalysisStmts with step.depth == 0
            //  when fix SAT-812: [JAVA] Wrong depth when exception thrown
            if (step.stmt in methodUnderAnalysisStmts && step.stmt !== entryPath.lastOrNull()?.stmt) {
                entryPath += step
            }
        }
        return entryPath
    }

    private fun getSymbolicPath(state: ExecutionState, symbolicResult: SymbolicResult): List<SymbolicStep> {
        val pathWithLines = state.fullPath().filter { step ->
            step.stmt.javaSourceStartLineNumber != -1
        }

        val symbolicSteps = pathWithLines.map { step ->
            val method = globalGraph.method(step.stmt)
            SymbolicStep(method, step.stmt.javaSourceStartLineNumber, step.depth)
        }.filter { step ->
            step.method.declaringClass.packageName == methodUnderTest.classId.packageName
        }

        return if (symbolicResult is SymbolicFailure && symbolicSteps.last().callDepth != 0) {
            // If we have the following case:
            // - method m1 calls method m2
            // - m2 calls m3
            // - m3 throws exception
            // then `symbolicSteps` suffix looks like:
            // - ...
            // - method = m3, lineNumber = .., callDepth = 2
            // - method = m3, lineNumber = 30, callDepth = 2 <- line with thrown exception
            // - method = m2, lineNumber = 20, callDepth = 1
            // - method = m1, lineNumber = 10, callDepth = 0
            // So, we want to remove 2 last entries (m1 and m2) because the execution finished at the line 30,
            // but `state.fullPath()` contains also reverse exits from methods after exception.
            // So, we need to remove the elements from the end of the list until the depth of the neighbors is the same.
            symbolicSteps
                .zipWithNext()
                .dropLastWhile { (cur, next) -> cur.callDepth != next.callDepth }
                .map { (cur, _) -> cur }
        } else {
            symbolicSteps
        }
    }
}

private fun ResolvedModels.constructStateForMethod(methodUnderTest: ExecutableId): EnvironmentModels {
    val (thisInstanceBefore, paramsBefore) = when {
        methodUnderTest.isStatic -> null to parameters
        methodUnderTest.isConstructor -> null to parameters.drop(1)
        else -> parameters.first() to parameters.drop(1)
    }
    return EnvironmentModels(thisInstanceBefore, paramsBefore, statics, methodUnderTest)
}

private suspend fun ConcreteExecutor<UtConcreteExecutionResult, Instrumentation<UtConcreteExecutionResult>>.executeConcretely(
    methodUnderTest: ExecutableId,
    stateBefore: EnvironmentModels,
    instrumentation: List<UtInstrumentation>,
    timeoutInMillis: Long
): UtConcreteExecutionResult = executeAsync(
    methodUnderTest.classId.name,
    methodUnderTest.signature,
    arrayOf(),
    parameters = UtConcreteExecutionData(
        stateBefore,
        instrumentation,
        timeoutInMillis
    )
).convertToAssemble(methodUnderTest.classId.packageName)

/**
 * Before pushing our states for concrete execution, we have to be sure that every state is consistent.
 * For now state could be inconsistent in case MUT parameters are wrappers that are not fully visited.
 * For example, not fully visited map can contain duplicate keys that leads to incorrect behaviour.
 * To prevent it, we need to add visited constraint for each MUT parameter-wrapper in state.
 */
private fun ExecutionState.withWrapperConsistencyChecks(): ExecutionState {
    val visitedConstraints = mutableSetOf<UtBoolExpression>()
    val methodUnderTestWrapperParameters = methodUnderTestParameters.filterNot { it.asWrapperOrNull == null }
    val methodUnderTestWrapperParametersAddresses = methodUnderTestWrapperParameters.map { it.addr }.toSet()

    if (methodUnderTestWrapperParameters.isEmpty()) {
        return this
    }

    // make consistency checks for parameters-wrappers ...
    methodUnderTestWrapperParameters.forEach { symbolicValue ->
        symbolicValue.asWrapperOrNull?.let {
            makeWrapperConsistencyCheck(symbolicValue, memory, visitedConstraints)
        }
    }

    // ... and all locals that depends on these parameters-wrappers
    val localReferenceValues = localVariableMemory
        .localValues
        .filterIsInstance<ReferenceValue>()
        .filter { it.addr.internal is UtArraySelectExpression }
    localReferenceValues.forEach {
        val theMostNestedAddr = findTheMostNestedAddr(it.addr.internal as UtArraySelectExpression)
        if (theMostNestedAddr in methodUnderTestWrapperParametersAddresses) {
            makeWrapperConsistencyCheck(it, memory, visitedConstraints)
        }
    }

    return copy(symbolicState = symbolicState + visitedConstraints.asHardConstraint())
}

private fun makeWrapperConsistencyCheck(
    symbolicValue: SymbolicValue,
    memory: Memory,
    visitedConstraints: MutableSet<UtBoolExpression>
) {
    val visitedSelectExpression = memory.isVisited(symbolicValue.addr)
    visitedConstraints += mkEq(visitedSelectExpression, mkInt(1))
}

private fun UtConcreteExecutionResult.violatesUtMockAssumption(): Boolean {
    // We should compare FQNs instead of `if (... is UtMockAssumptionViolatedException)`
    // because the exception from the `concreteExecutionResult` is loaded by user's ClassLoader,
    // but the `UtMockAssumptionViolatedException` is loaded by the current ClassLoader,
    // so we can't cast them to each other.
    return result.exceptionOrNull()?.javaClass?.name == UtMockAssumptionViolatedException::class.java.name
}

private fun UtConcreteExecutionResult.processedFailure(): UtConcreteExecutionProcessedFailure?
 = result as? UtConcreteExecutionProcessedFailure

private fun checkStaticMethodsMock(execution: UtSymbolicExecution) =
    execution.instrumentation.any { it is UtStaticMethodInstrumentation}