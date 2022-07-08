package org.utbot.engine

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.yield
import mu.KotlinLogging
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.FeatureProcessor
import org.utbot.analytics.Predictors
import org.utbot.common.WorkaroundReason.REMOVE_ANONYMOUS_CLASSES
import org.utbot.common.bracket
import org.utbot.common.debug
import org.utbot.common.workaround
import org.utbot.engine.MockStrategy.NO_MOCKS
import org.utbot.engine.pc.UtArraySelectExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtContextInitializer
import org.utbot.engine.pc.UtSolver
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.engine.pc.findTheMostNestedAddr
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkInt
import org.utbot.engine.selectors.PathSelector
import org.utbot.engine.selectors.StrategyOption
import org.utbot.engine.selectors.coveredNewSelector
import org.utbot.engine.selectors.cpInstSelector
import org.utbot.engine.selectors.forkDepthSelector
import org.utbot.engine.selectors.inheritorsSelector
import org.utbot.engine.selectors.nnRewardGuidedSelector
import org.utbot.engine.selectors.nurs.NonUniformRandomSearch
import org.utbot.engine.selectors.pollUntilFastSAT
import org.utbot.engine.selectors.randomPathSelector
import org.utbot.engine.selectors.randomSelector
import org.utbot.engine.selectors.strategies.GraphViz
import org.utbot.engine.selectors.subpathGuidedSelector
import org.utbot.engine.symbolic.SymbolicState
import org.utbot.engine.symbolic.asHardConstraint
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
import org.utbot.framework.concrete.UtConcreteExecutionData
import org.utbot.framework.concrete.UtConcreteExecutionResult
import org.utbot.framework.concrete.UtExecutionInstrumentation
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtResult
import org.utbot.framework.plugin.api.graph
import org.utbot.framework.plugin.api.jimpleBody
import org.utbot.framework.plugin.api.onSuccess
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.description
import org.utbot.fuzzer.FallbackModelProvider
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.collectConstantsForFuzzer
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.names.MethodBasedNameSuggester
import org.utbot.fuzzer.names.ModelBasedNameSuggester
import org.utbot.instrumentation.ConcreteExecutor
import soot.jimple.Stmt
import soot.tagkit.ParamNamesTag
import java.lang.reflect.Method
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}
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

//all id values of synthetic default models must be greater that for real ones
private var nextDefaultModelId = 1500_000_000

private fun pathSelector(graph: InterProceduralUnitGraph, typeRegistry: TypeRegistry) =
    when (pathSelectorType) {
        PathSelectorType.COVERED_NEW_SELECTOR -> coveredNewSelector(graph) {
            withStepsLimit(pathSelectorStepsLimit)
        }
        PathSelectorType.INHERITORS_SELECTOR -> inheritorsSelector(graph, typeRegistry) {
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
        PathSelectorType.NN_REWARD_GUIDED_SELECTOR -> nnRewardGuidedSelector(graph, StrategyOption.DISTANCE) {
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
    private val methodUnderTest: UtMethod<*>,
    classpath: String,
    dependencyPaths: String,
    mockStrategy: MockStrategy = NO_MOCKS,
    chosenClassesToMockAlways: Set<ClassId>,
    private val solverTimeoutInMillis: Int = checkSolverTimeoutMillis
) : UtContextInitializer() {

    private val graph = jimpleBody(methodUnderTest).also {
        logger.trace { "JIMPLE for $methodUnderTest:\n$this" }
    }.graph()

    private val methodUnderAnalysisStmts: Set<Stmt> = graph.stmts.toSet()
    private val globalGraph = InterProceduralUnitGraph(graph)
    private val typeRegistry: TypeRegistry = TypeRegistry()
    private val pathSelector: PathSelector = pathSelector(globalGraph, typeRegistry)

    internal val hierarchy: Hierarchy = Hierarchy(typeRegistry)

    // TODO HACK violation of encapsulation
    internal val typeResolver: TypeResolver = TypeResolver(typeRegistry, hierarchy)

    private val classUnderTest: ClassId = methodUnderTest.clazz.id

    private val mocker: Mocker = Mocker(
        mockStrategy,
        classUnderTest,
        hierarchy,
        chosenClassesToMockAlways,
        MockListenerController(controller)
    )

    fun attachMockListener(mockListener: MockListener) = mocker.mockListenerController?.attach(mockListener)

    private val statesForConcreteExecution: MutableList<ExecutionState> = mutableListOf()

    private val traverser = Traverser(
        methodUnderTest,
        typeRegistry,
        hierarchy,
        typeResolver,
        globalGraph,
        mocker,
    )

    //HACK (long strings)
    internal var softMaxArraySize = 40

    private val concreteExecutor =
        ConcreteExecutor(
            UtExecutionInstrumentation,
            classpath,
            dependencyPaths
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

                if (controller.executeConcretely || statesForConcreteExecution.isNotEmpty()) {
                    val state = pathSelector.pollUntilFastSAT()
                        ?: statesForConcreteExecution.pollUntilSat(processUnknownStatesDuringConcreteExecution)
                        ?: break
                    // This state can contain inconsistent wrappers - for example, Map with keys but missing values.
                    // We cannot use withWrapperConsistencyChecks here because it needs solver to work.
                    // So, we have to process such cases accurately in wrappers resolving.

                    logger.trace { "executing $state concretely..." }


                    logger.debug().bracket("concolicStrategy<$methodUnderTest>: execute concretely") {
                        val resolver = Resolver(
                            hierarchy,
                            state.memory,
                            typeRegistry,
                            typeResolver,
                            state.solver.lastStatus as UtSolverStatusSAT,
                            methodUnderTest,
                            softMaxArraySize
                        )

                        val resolvedParameters = state.methodUnderTestParameters
                        val (modelsBefore, _, instrumentation) = resolver.resolveModels(resolvedParameters)
                        val stateBefore = modelsBefore.constructStateForMethod(methodUnderTest)

                        try {
                            val concreteExecutionResult =
                                concreteExecutor.executeConcretely(methodUnderTest, stateBefore, instrumentation)

                            val concreteUtExecution = UtExecution(
                                stateBefore,
                                concreteExecutionResult.stateAfter,
                                concreteExecutionResult.result,
                                instrumentation,
                                mutableListOf(),
                                listOf(),
                                concreteExecutionResult.coverage
                            )
                            emit(concreteUtExecution)

                            logger.debug { "concolicStrategy<${methodUnderTest}>: returned $concreteUtExecution" }
                        } catch (e: CancellationException) {
                            logger.debug(e) { "Cancellation happened" }
                        } catch (e: ConcreteExecutionFailureException) {
                            emitFailedConcreteExecutionResult(stateBefore, e)
                        } catch (e: Throwable) {
                            emit(UtError("Concrete execution failed", e))
                        }
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
                }
            }
        }
    }


    /**
     * Run fuzzing flow.
     *
     * @param until is used by fuzzer to cancel all tasks if the current time is over this value
     * @param modelProvider provides model values for a method
     */
    fun fuzzing(until: Long = Long.MAX_VALUE, modelProvider: (ModelProvider) -> ModelProvider = { it }) = flow {
        val executableId = if (methodUnderTest.isConstructor) {
            methodUnderTest.javaConstructor!!.executableId
        } else {
            methodUnderTest.javaMethod!!.executableId
        }

        val isFuzzable = executableId.parameters.all { classId ->
            classId != Method::class.java.id && // causes the child process crash at invocation
                classId != Class::class.java.id  // causes java.lang.IllegalAccessException: java.lang.Class at sun.misc.Unsafe.allocateInstance(Native Method)
        }
        if (!isFuzzable) {
            return@flow
        }

        val fallbackModelProvider = FallbackModelProvider { nextDefaultModelId++ }

        val thisInstance = when {
            methodUnderTest.isStatic -> null
            methodUnderTest.isConstructor -> if (
                methodUnderTest.clazz.isAbstract ||  // can't instantiate abstract class
                methodUnderTest.clazz.java.isEnum    // can't reflectively create enum objects
            ) {
                return@flow
            } else {
                null
            }
            else -> {
                fallbackModelProvider.toModel(methodUnderTest.clazz).apply {
                    if (this is UtNullModel) { // it will definitely fail because of NPE,
                        return@flow
                    }
                }
            }
        }

        val methodUnderTestDescription = FuzzedMethodDescription(executableId, collectConstantsForFuzzer(graph)).apply {
            compilableName = if (methodUnderTest.isMethod) executableId.name else null
            val names = graph.body.method.tags.filterIsInstance<ParamNamesTag>().firstOrNull()?.names
            parameterNameMap = { index -> names?.getOrNull(index) }
        }
        val modelProviderWithFallback = modelProvider(defaultModelProviders { nextDefaultModelId++ }).withFallback(fallbackModelProvider::toModel)
        val coveredInstructionTracker = mutableSetOf<Instruction>()
        var attempts = UtSettings.fuzzingMaxAttempts
        fuzz(methodUnderTestDescription, modelProviderWithFallback).forEach { values ->
            if (System.currentTimeMillis() >= until) {
                logger.info { "Fuzzing overtime: $methodUnderTest" }
                return@flow
            }

            val initialEnvironmentModels = EnvironmentModels(thisInstance, values.map { it.model }, mapOf())

            try {
                val concreteExecutionResult =
                    concreteExecutor.executeConcretely(methodUnderTest, initialEnvironmentModels, listOf())

                workaround(REMOVE_ANONYMOUS_CLASSES) {
                    concreteExecutionResult.result.onSuccess {
                        if (it.classId.isAnonymous) {
                            logger.debug("Anonymous class found as a concrete result, symbolic one will be returned")
                            return@flow
                        }
                    }
                }

                if (!coveredInstructionTracker.addAll(concreteExecutionResult.coverage.coveredInstructions)) {
                    if (--attempts < 0) {
                        return@flow
                    }
                }

                val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester())
                val testMethodName = try {
                    nameSuggester.flatMap { it.suggest(methodUnderTestDescription, values, concreteExecutionResult.result) }.firstOrNull()
                } catch (t: Throwable) {
                    logger.error(t) { "Cannot create suggested test name for ${methodUnderTest.displayName}" }
                    null
                }

                emit(
                    UtExecution(
                        stateBefore = initialEnvironmentModels,
                        stateAfter = concreteExecutionResult.stateAfter,
                        result = concreteExecutionResult.result,
                        instrumentation = emptyList(),
                        path = mutableListOf(),
                        fullPath = emptyList(),
                        coverage = concreteExecutionResult.coverage,
                        testMethodName = testMethodName?.testName,
                        displayName = testMethodName?.displayName
                    )
                )
            } catch (e: CancellationException) {
                logger.debug { "Cancelled by timeout" }
            } catch (e: ConcreteExecutionFailureException) {
                emitFailedConcreteExecutionResult(initialEnvironmentModels, e)
            } catch (e: Throwable) {
                emit(UtError("Default concrete execution failed", e))
            }
        }
    }

    private suspend fun FlowCollector<UtResult>.emitFailedConcreteExecutionResult(
        stateBefore: EnvironmentModels,
        e: ConcreteExecutionFailureException
    ) {
        val failedConcreteExecution = UtExecution(
            stateBefore = stateBefore,
            stateAfter = MissingState,
            result = UtConcreteExecutionFailure(e),
            instrumentation = emptyList(),
            path = mutableListOf(),
            fullPath = listOf()
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
        val holder = requireNotNull(solver.lastStatus as? UtSolverStatusSAT) { "The state must be SAT!" }

        val predictedTestName = Predictors.testName.predict(state.path)
        Predictors.testName.provide(state.path, predictedTestName, "")

        // resolving
        val resolver =
            Resolver(hierarchy, memory, typeRegistry, typeResolver, holder, methodUnderTest, softMaxArraySize)

        val (modelsBefore, modelsAfter, instrumentation) = resolver.resolveModels(parameters)

        val symbolicExecutionResult = resolver.resolveResult(symbolicResult)

        val stateBefore = modelsBefore.constructStateForMethod(methodUnderTest)
        val stateAfter = modelsAfter.constructStateForMethod(methodUnderTest)
        require(stateBefore.parameters.size == stateAfter.parameters.size)

        val symbolicUtExecution = UtExecution(
            stateBefore,
            stateAfter,
            symbolicExecutionResult,
            instrumentation,
            entryMethodPath(state),
            state.fullPath()
        )

        globalGraph.traversed(state)

        if (!UtSettings.useConcreteExecution ||
            // Can't execute concretely because overflows do not cause actual exceptions.
            // Still, we need overflows to act as implicit exceptions.
            (UtSettings.treatOverflowAsError && symbolicExecutionResult is UtOverflowFailure)
        ) {
            logger.debug {
                "processResult<${methodUnderTest}>: no concrete execution allowed, " +
                        "emit purely symbolic result $symbolicUtExecution"
            }
            emit(symbolicUtExecution)
            return
        }

        //It's possible that symbolic and concrete stateAfter/results are diverged.
        //So we trust concrete results more.
        try {
            logger.debug().bracket("processResult<$methodUnderTest>: concrete execution") {

                //this can throw CancellationException
                val concreteExecutionResult = concreteExecutor.executeConcretely(
                    methodUnderTest,
                    stateBefore,
                    instrumentation
                )

                workaround(REMOVE_ANONYMOUS_CLASSES) {
                    concreteExecutionResult.result.onSuccess {
                        if (it.classId.isAnonymous) {
                            logger.debug("Anonymous class found as a concrete result, symbolic one will be returned")
                            emit(symbolicUtExecution)
                            return
                        }
                    }
                }

                val concolicUtExecution = symbolicUtExecution.copy(
                    stateAfter = concreteExecutionResult.stateAfter,
                    result = concreteExecutionResult.result,
                    coverage = concreteExecutionResult.coverage
                )

                emit(concolicUtExecution)
                logger.debug { "processResult<${methodUnderTest}>: returned $concolicUtExecution" }
            }
        } catch (e: ConcreteExecutionFailureException) {
            emitFailedConcreteExecutionResult(stateBefore, e)
        }
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
}

private fun ResolvedModels.constructStateForMethod(methodUnderTest: UtMethod<*>): EnvironmentModels {
    val (thisInstanceBefore, paramsBefore) = when {
        methodUnderTest.isStatic -> null to parameters
        methodUnderTest.isConstructor -> null to parameters.drop(1)
        else -> parameters.first() to parameters.drop(1)
    }
    return EnvironmentModels(thisInstanceBefore, paramsBefore, statics)
}

private suspend fun ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>.executeConcretely(
    methodUnderTest: UtMethod<*>,
    stateBefore: EnvironmentModels,
    instrumentation: List<UtInstrumentation>
): UtConcreteExecutionResult = executeAsync(
    methodUnderTest.callable,
    arrayOf(),
    parameters = UtConcreteExecutionData(stateBefore, instrumentation)
).convertToAssemble(methodUnderTest)

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
