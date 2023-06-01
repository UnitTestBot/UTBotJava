package org.utbot.engine

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.FeatureProcessor
import org.utbot.analytics.Predictors
import org.utbot.api.exception.UtMockAssumptionViolatedException
import org.utbot.common.measureTime
import org.utbot.common.debug
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
import org.utbot.framework.util.convertToAssemble
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.graph
import org.utbot.framework.util.sootMethod
import org.utbot.fuzzer.*
import org.utbot.fuzzing.*
import org.utbot.fuzzing.providers.AutowiredValueProvider
import org.utbot.fuzzing.type.factories.SimpleFuzzedTypeFactory
import org.utbot.fuzzing.type.factories.SpringFuzzedTypeFactory
import org.utbot.fuzzing.utils.Trie
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import soot.jimple.Stmt
import soot.tagkit.ParamNamesTag
import java.lang.reflect.Method
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
    executionInstrumentation: Instrumentation<UtConcreteExecutionResult>,
    private val solverTimeoutInMillis: Int = checkSolverTimeoutMillis
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
        applicationContext = applicationContext,
    )

    fun attachMockListener(mockListener: MockListener) = mocker.mockListenerController?.attach(mockListener)

    fun detachMockListener(mockListener: MockListener) = mocker.mockListenerController?.detach(mockListener)

    private val statesForConcreteExecution: MutableList<ExecutionState> = mutableListOf()

    private val traverser = Traverser(
        methodUnderTest,
        typeRegistry,
        hierarchy,
        typeResolver,
        globalGraph,
        mocker,
        applicationContext,
    )

    //HACK (long strings)
    internal var softMaxArraySize = 40

    private val concreteExecutor =
        ConcreteExecutor(
            executionInstrumentation,
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
     * @param transform provides model values for a method
     */
    fun fuzzing(until: Long = Long.MAX_VALUE, transform: (JavaValueProvider) -> JavaValueProvider = { it }) = flow {
        val isFuzzable = methodUnderTest.parameters.all { classId ->
            classId != Method::class.java.id && // causes the instrumented process crash at invocation
                classId != Class::class.java.id  // causes java.lang.IllegalAccessException: java.lang.Class at sun.misc.Unsafe.allocateInstance(Native Method)
        }
        if (!isFuzzable) {
            // Currently, fuzzer doesn't work with static methods with empty parameters
            return@flow
        }
        val errorStackTraceTracker = Trie(StackTraceElement::toString)
        var attempts = 0
        val attemptsLimit = UtSettings.fuzzingMaxAttempts
        val names = graph.body.method.tags.filterIsInstance<ParamNamesTag>().firstOrNull()?.names ?: emptyList()
        var testEmittedByFuzzer = 0
        runJavaFuzzing(
            defaultIdGenerator,
            methodUnderTest,
            collectConstantsForFuzzer(graph),
            names,
            listOf(transform(ValueProvider.of(defaultValueProviders(defaultIdGenerator)))),
            fuzzedTypeFactory = when (applicationContext) {
                is SpringApplicationContext -> when (applicationContext.typeReplacementApproach) {
                    is TypeReplacementApproach.ReplaceIfPossible -> SpringFuzzedTypeFactory(
                        autowiredValueProvider = AutowiredValueProvider(
                            defaultIdGenerator,
                            autowiredModelOriginCreator = { beanName ->
                                runBlocking {
                                    logger.info { "Getting bean: $beanName" }
                                    concreteExecutor.withProcess { getBean(beanName) }
                                }
                            }
                        ),
                        beanNamesFinder = { classId ->
                            applicationContext.beanDefinitions
                                .filter { it.beanTypeFqn == classId.name }
                                .map { it.beanName }
                        }
                    )
                    is TypeReplacementApproach.DoNotReplace -> SimpleFuzzedTypeFactory()
                }
                else -> SimpleFuzzedTypeFactory()
            },
        ) { thisInstance, descr, values ->
            if (thisInstance?.model is UtNullModel) {
                // We should not try to run concretely any models with null-this.
                // But fuzzer does generate such values, because it can fail to generate any "good" values.
                return@runJavaFuzzing BaseFeedback(Trie.emptyNode(), Control.PASS)
            }

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
                return@runJavaFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
            }

            val initialEnvironmentModels = EnvironmentModels(thisInstance?.model, values.map { it.model }, mapOf())

            val concreteExecutionResult: UtConcreteExecutionResult? = try {
                val timeoutMillis = min(UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis, diff)
                concreteExecutor.executeConcretely(methodUnderTest, initialEnvironmentModels, listOf(), timeoutMillis)
            } catch (e: CancellationException) {
                logger.debug { "Cancelled by timeout" }; null
            } catch (e: InstrumentedProcessDeathException) {
                emitFailedConcreteExecutionResult(initialEnvironmentModels, e); null
            } catch (e: Throwable) {
                emit(UtError("Default concrete execution failed", e)); null
            }

            // in case an exception occurred from the concrete execution
            concreteExecutionResult ?: return@runJavaFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)

            if (concreteExecutionResult.violatesUtMockAssumption()) {
                logger.debug { "Generated test case by fuzzer violates the UtMock assumption: $concreteExecutionResult" }
                return@runJavaFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
            }

            val coveredInstructions = concreteExecutionResult.coverage.coveredInstructions
            var trieNode: Trie.Node<Instruction>? = null
            if (coveredInstructions.isNotEmpty()) {
                trieNode = descr.tracer.add(coveredInstructions)
                if (trieNode.count > 1) {
                    if (++attempts >= attemptsLimit) {
                        return@runJavaFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                    }
                    return@runJavaFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                }
            } else {
                logger.error { "Coverage is empty for $methodUnderTest with $values" }
                val result = concreteExecutionResult.result
                if (result is UtSandboxFailure) {
                    val stackTraceElements = result.exception.stackTrace.reversed()
                    if (errorStackTraceTracker.add(stackTraceElements).count > 1) {
                        return@runJavaFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                    }
                }
            }

            emit(
                UtFuzzedExecution(
                    stateBefore = initialEnvironmentModels,
                    stateAfter = concreteExecutionResult.stateAfter,
                    result = concreteExecutionResult.result,
                    coverage = concreteExecutionResult.coverage,
                    fuzzingValues = values,
                    fuzzedMethodDescription = descr.description,
                    instrumentation = concreteExecutionResult.newInstrumentation ?: emptyList()
                )
            )

            testEmittedByFuzzer++
            BaseFeedback(result = trieNode ?: Trie.emptyNode(), control = Control.CONTINUE)
        }
    }

    private suspend fun FlowCollector<UtResult>.emitFailedConcreteExecutionResult(
        stateBefore: EnvironmentModels,
        e: InstrumentedProcessDeathException
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
        val holder = requireNotNull(solver.lastStatus as? UtSolverStatusSAT) { "The state must be SAT!" }

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
            fullPath = state.fullPath()
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
            emit(UtError("Default concrete execution failed", e));
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

private fun ResolvedModels.constructStateForMethod(methodUnderTest: ExecutableId): EnvironmentModels {
    val (thisInstanceBefore, paramsBefore) = when {
        methodUnderTest.isStatic -> null to parameters
        methodUnderTest.isConstructor -> null to parameters.drop(1)
        else -> parameters.first() to parameters.drop(1)
    }
    return EnvironmentModels(thisInstanceBefore, paramsBefore, statics)
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

private fun checkStaticMethodsMock(execution: UtSymbolicExecution) =
    execution.instrumentation.any { it is UtStaticMethodInstrumentation}