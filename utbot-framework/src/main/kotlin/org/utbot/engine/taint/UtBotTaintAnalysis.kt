package org.utbot.engine.taint

import com.jetbrains.rd.util.concurrentMapOf
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.utbot.engine.EngineController
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.Mocker
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.jimpleBody
import org.utbot.engine.logger
import org.utbot.engine.pathSelector
import org.utbot.engine.selectors.strategies.DistanceStatistics
import org.utbot.engine.selectors.strategies.StepsLimitStoppingStrategy
import org.utbot.engine.selectors.taint.NeverDroppingStrategy
import org.utbot.engine.selectors.taint.NewTaintPathSelector
import org.utbot.engine.taint.priority.SimpleTaintMethodsAnalysisPrioritizer
import org.utbot.engine.taint.priority.TaintMethodsAnalysisPrioritizer
import org.utbot.engine.taint.timeout.SimpleTaintTimeoutStrategy
import org.utbot.engine.taint.timeout.TaintTimeoutStrategy
import org.utbot.framework.AnalysisMode
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator.ExecutionTimeEstimator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.defaultTestFlow
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.util.graph
import org.utbot.framework.util.sootMethod
import org.utbot.framework.util.toModel
import soot.SootMethod
import soot.jimple.Stmt
import java.io.File
import java.net.URL
import java.net.URLClassLoader

// TODO find a better name
typealias TaintPairs = Map<TaintSourceData, Set<TaintSinkData>>

typealias TaintCandidates = Map<ExecutableId, TaintPairs>

typealias TaintPath = List<Stmt>

class UtBotTaintAnalysis(private val taintConfiguration: TaintConfiguration) {

    // TODO configure via Settings
    private val timeoutStrategy: TaintTimeoutStrategy = SimpleTaintTimeoutStrategy

    // TODO configure via Settings
    private val taintMethodsAnalysisPrioritizer: TaintMethodsAnalysisPrioritizer = SimpleTaintMethodsAnalysisPrioritizer

    // TODO should it be something different for taint?
    private val mockStrategy: MockStrategyApi = MockStrategyApi.THIRD_PARTY_LIBRARY_CLASSES

    init {
        passTaintAnalysisConfiguration()
    }

    private fun passTaintAnalysisConfiguration() {
        // TODO use configuration in TaintAnalysis
    }

    // TODO copy-paste from org.utbot.cli.util.ClassLoaderUtilsKt.toUrl
    private fun String.toUrl(): URL = File(this).toURI().toURL()

    // TODO copy-paste from org.utbot.cli.util.ClassLoaderUtilsKt.createClassLoader
    private fun createClassLoader(
        classPath: String? = "",
        absoluteFileNameWithClasses: String? = null
    ): URLClassLoader {
        val urlSet = mutableSetOf<URL>()
        classPath?.run {
            urlSet.addAll(this.split(File.pathSeparatorChar).map { it.toUrl() }.toMutableSet())
        }
        absoluteFileNameWithClasses?.run {
            urlSet.addAll(File(absoluteFileNameWithClasses).readLines().map { it.toUrl() }.toMutableSet())
        }
        val urls = urlSet.toTypedArray()
        return URLClassLoader(urls)
    }

    data class ControllerWithTimeEstimator(
        val controller: EngineController,
        val timeEstimator: TaintTimeEstimator
    )

    sealed class AnalysisStopReason(val elapsedTimeMs: Long, private val explanation: String) {

        override fun toString(): String = "$explanation, elapsed time $elapsedTimeMs (ms)"

        class COVERAGE(elapsedTimeMs: Long) : AnalysisStopReason(elapsedTimeMs, explanation = "All covered")
        class TIMEOUT(elapsedTimeMs: Long) : AnalysisStopReason(elapsedTimeMs, explanation = "Timeout exceeding")
        class STEPS(elapsedTimeMs: Long) :
            AnalysisStopReason(elapsedTimeMs, explanation = "Step limit ${UtSettings.pathSelectorStepsLimit} exceeding")

        class ERRORS(elapsedTimeMs: Long) :
            AnalysisStopReason(elapsedTimeMs, explanation = "Errors during analysis occurred")

        class DISCOVERED_ALL_TAINTS(elapsedTimeMs: Long) :
            AnalysisStopReason(elapsedTimeMs, explanation = "Found all passed taints")

        class WAS_NOT_STARTED() : AnalysisStopReason(elapsedTimeMs = 0L, explanation = "Was not started")
    }

    data class TaintTimeEstimator(val initialTimeBudgetMs: Long) {
        private var additionalTimeBudgetMs: Long = 0
        private var startTimeMs: Long = System.currentTimeMillis()

        fun setStart() {
            startTimeMs = System.currentTimeMillis()
        }

        fun addTimeBudget(timeBudgetMs: Long) {
            additionalTimeBudgetMs += timeBudgetMs.coerceAtLeast(0)
        }

        val totalTimeBudgetMs: Long
            get() = initialTimeBudgetMs + additionalTimeBudgetMs

        val elapsedTimeMs: Long
            get() = System.currentTimeMillis() - startTimeMs

        val remainingTimeBudgetMs: Long
            get() = totalTimeBudgetMs - elapsedTimeMs

        fun isTimeElapsed(): Boolean = remainingTimeBudgetMs <= 0
    }

    private fun getUpdatedSolverCheckTimeoutMs(totalTimeBudgetMs: Long): Int =
        if (totalTimeBudgetMs < UtSettings.checkSolverTimeoutMillis) {
            totalTimeBudgetMs.toInt()
        } else {
            UtSettings.checkSolverTimeoutMillis
        }


    // TODO mostly copy-paste from org.utbot.framework.plugin.api.TestCaseGenerator#generate, perhaps it could be simplified
    @OptIn(DelicateCoroutinesApi::class)
    private fun runTaintAnalysisJobs(
        sortedByPriorityTaintMethodsWithTimeouts: List<TaintMethodWithTimeout>,
        mutToSourceSinksPairs: MutableMap<ExecutableId, MutableMap<Stmt, MutableSet<Stmt>>>,
        taintCandidates: TaintCandidates,
        encounteredMethods: Set<SootMethod>,
        currentUtContext: UtContext,
        mockAlwaysDefaults: Set<ClassId>,
        classPath: String,
        dependencyPaths: String,
        totalTimeoutMs: Long
    ): Pair<MutableMap<ExecutableId, MutableList<UtSymbolicExecution>>, MutableMap<ExecutableId, AnalysisStopReason>> {
        disableCoroutinesDebug()

        val totalExecutionEndTime = System.currentTimeMillis() + totalTimeoutMs

        var toBeProcessed = sortedByPriorityTaintMethodsWithTimeouts.size

        // Concurrent seems to be required here
        val analysisStopReasons = concurrentMapOf<ExecutableId, AnalysisStopReason>()
        val executionsByExecutable = mutableMapOf<ExecutableId, MutableList<UtSymbolicExecution>>().apply {
            // Fill with empty lists of executions for logging later
            sortedByPriorityTaintMethodsWithTimeouts.forEach {
                put(it.method, mutableListOf())
            }
        }

        for ((method, _) in sortedByPriorityTaintMethodsWithTimeouts) {
            val taintsToBeFound = mutToSourceSinksPairs.getValue(method)

            val mutStartTime = System.currentTimeMillis()
            if (mutStartTime > totalExecutionEndTime) {
                executionsByExecutable.keys.forEach {
                    analysisStopReasons.putIfAbsent(it, AnalysisStopReason.WAS_NOT_STARTED())
                }

                logger.warn { "Total timeout $totalTimeoutMs (ms) exceeded, analysis is canceled" }
                break
            }

            val mutEndTime = mutStartTime + (totalExecutionEndTime - mutStartTime) / toBeProcessed

            val controller = object : EngineController() {
                override var stop: Boolean
                    get() = System.currentTimeMillis() >= mutEndTime || taintsToBeFound.isEmpty()
                    set(value) {}
            }

            try {
                withUtContext(currentUtContext) {
                    runBlocking {
                        val engine = UtBotSymbolicEngine(
                            controller,
                            method,
                            classPath,
                            dependencyPaths,
                            mockStrategy = mockStrategy.toModel(),
                            chosenClassesToMockAlways = mockAlwaysDefaults,
                            solverTimeoutInMillis = getUpdatedSolverCheckTimeoutMs(mutEndTime - mutStartTime)
                        )
                        engine.addMethodsToDoNotMock(encounteredMethods)

                        controller.job = currentCoroutineContext().job
                        val graph = method.sootMethod.jimpleBody().graph()
                        val globalGraph = InterProceduralUnitGraph(graph)
                        engine.pathSelector = if (UtSettings.pathSelectorType == PathSelectorType.NEW_TAINT_SELECTOR) {
                            NewTaintPathSelector(
                                graph,
                                taintCandidates[method] ?: emptyMap(),
                                NeverDroppingStrategy(globalGraph),
                                StepsLimitStoppingStrategy(3500, globalGraph)
                            )
                        } else {
                            pathSelector(engine.globalGraph, engine.typeRegistry)
                        }
                        runEngine(engine, method, analysisStopReasons, executionsByExecutable, taintsToBeFound)
                    }
                }
            } catch (e: Throwable) {
                logger.error(e) { "Error in flow during taint analysis" }
            } finally {
                toBeProcessed--
            }
        }

        executionsByExecutable.entries.forEach {
            logger.warn {
                "Method: ${it.key}, number of executions: ${it.value.size}, end of analysis reason: ${analysisStopReasons[it.key]}"
            }
        }

        return executionsByExecutable to analysisStopReasons
    }

    private suspend fun runEngine(
        engine: UtBotSymbolicEngine,
        method: ExecutableId,
        analysisStopReasons: MutableMap<ExecutableId, AnalysisStopReason>,
        executionsByExecutable: MutableMap<ExecutableId, MutableList<UtSymbolicExecution>>,
        taintsToBeFound: MutableMap<Stmt, MutableSet<Stmt>>
    ) {
        val engineFlow = engine.traverse()
        val methodStartTime = System.currentTimeMillis()

        engineFlow.onStart {
            logger.warn { "Started symbolic analysis of the executable $method" }
        }.onCompletion { error ->
            if (error != null) {
                logger.error(error) { "Error in taint flow for $method" }
                return@onCompletion
            }

            val elapsedTimeMs = System.currentTimeMillis() - methodStartTime

            analysisStopReasons.getOrPut(method) {
                when {
                    taintsToBeFound.isEmpty() -> {
                        logger.warn {
                            "Taint analysis of the executable $method was stopped since it found all provided taints, " +
                                    "elapsed time $elapsedTimeMs (ms)"
                        }

                        AnalysisStopReason.DISCOVERED_ALL_TAINTS(elapsedTimeMs)
                    }
                    engine.wasStoppedByController -> {
                        logger.warn {
                            "Taint analysis of the executable $method was stopped due to exceeding time limit, " +
                                    "elapsed time $elapsedTimeMs (ms)"
                        }

                        AnalysisStopReason.TIMEOUT(elapsedTimeMs)
                    }
                    engine.wasStoppedByStepsLimit -> {
                        logger.warn {
                            "Taint analysis of the executable $method was stopped due to exceeding steps limit, " +
                                    "elapsed time $elapsedTimeMs (ms)"
                        }

                        AnalysisStopReason.STEPS(elapsedTimeMs)
                    }
                    else -> {
                        logger.warn {
                            "Taint analysis of the executable $method ended due to full coverage, " +
                                    "elapsed time $elapsedTimeMs (ms)"
                        }

                        AnalysisStopReason.COVERAGE(elapsedTimeMs)
                    }
                }
            }
        }.catch {
            logger.error(it) { "Error in flow during taint analysis" }
        }.collect { result ->
            when (result) {
                is UtSymbolicExecution -> {
                    executionsByExecutable.getValue(method) += result

                    logger.warn { "Method: ${method}, executions: $result" }

                    val executionResult = result.result
                    if (executionResult !is UtExecutionFailure) {
                        return@collect
                    }

                    val exception = executionResult.exception
                    if (exception !is TaintAnalysisError) {
                        return@collect
                    }

                    val path = result.fullPath.map { it.stmt }
                    val sink = exception.taintSink
                    val source = retrieveSource(path, taintsToBeFound, sink) ?: return@collect

                    (engine.pathSelector as? NewTaintPathSelector)?.let {
                        // Mark source/sink pairs as visited
                        it.visitedTaintPairs += NewTaintPathSelector.TaintPair(source, sink)
                    }

                    taintsToBeFound[source]?.remove(sink)
                    if (taintsToBeFound[source]?.isEmpty() == true) {
                        taintsToBeFound.remove(source)
                    }
                }
                is UtError -> {
                    val elapsedTimeMs = System.currentTimeMillis() - methodStartTime
                    analysisStopReasons.putIfAbsent(method, AnalysisStopReason.ERRORS(elapsedTimeMs))

                    logger.error(result.error) { "Failed to analyze for taint: $result" }
                }
                else -> logger.error { "Unexpected taint execution $result" }
            }
        }
    }

    fun runTaintAnalysis(
        taintCandidates: TaintCandidates,
        encounteredMethods: Set<SootMethod>,
        totalTimeoutMs: Long,
        classPath: String
    ): MutableMap<ExecutableId, UtBotExecutableTaints> {
        disableCoroutinesDebug()

        val timeouts = timeoutStrategy.splitTimeout(totalTimeoutMs, taintCandidates)
        val taintsWithTimeout = timeouts.map { (method, timeout) ->
            TaintMethodWithTimeout(method, taintCandidates[method]!!, timeout)
        }
        val sortedByPriorityTaintMethods = taintMethodsAnalysisPrioritizer.sortByPriority(taintsWithTimeout)

        UtSettings.useTaintAnalysisMode = true
        UtSettings.useFuzzing = false
        UtSettings.useSandbox = false
        UtSettings.useConcreteExecution = false
        UtSettings.useCustomJavaDocTags = false
        UtSettings.enableSummariesGeneration = false
        UtSettings.checkNpeInNestedNotPrivateMethods = true
        UtSettings.preferredCexOption = false
        if (taintCandidates.size < UtSettings.taintPrecisionThreshold) {
            UtSettings.setLessPrecision = true
        }

        // Some classes are missed in the classpath so we cannot construct an assemble model for them
        UtSettings.useAssembleModelGenerator = false

        AnalysisMode.TAINT.applyMode()
        // TODO move to the TAINT analysis mode?
        UtSettings.useOnlyTaintAnalysis = true

        val mockAlwaysDefaults = Mocker.javaDefaultClassesToMockAlways.mapTo(mutableSetOf()) { it.id }
        val (classpathForEngine, dependencyPaths) = retrieveClassPaths()
        val executableTaints = mutableMapOf<ExecutableId, UtBotExecutableTaints>()

        val knownTaintSources = taintCandidates.values.flatMap { it.keys }.associate { it.stmt to it.taintKinds }
        taintAnalysis.setConfiguration(taintConfiguration)
        taintAnalysis.addKnownSourceStatements(knownTaintSources)

        val classPath1 = "$classPath${File.pathSeparatorChar}${System.getProperty("java.class.path")}"
        val classLoader = createClassLoader(classPath1)

        val mutToSourseSinksPairs = taintCandidates.mapValuesTo(mutableMapOf()) {
            it.value.mapKeys { it.key.stmt }.mapValuesTo(mutableMapOf()) { it.value.mapTo(mutableSetOf()) { it.stmt } }
        }

        val immutableMutToSourseSinksPairs =
            mutToSourseSinksPairs.mapValues { it.value.mapValues { it.value.toSet() }.toMap() }.toMap()

        /*runTaintAnalysisWithOneController(
            sortedByPriorityTaintMethods,
            classLoader,
            classpathForEngine,
            dependencyPaths,
            mockAlwaysDefaults,
            confirmedTaints
        )*/

        val currentUtContext = UtContext(classLoader)
        val (executionsByExecutable, analysisStopReasons) = runTaintAnalysisJobs(
            sortedByPriorityTaintMethods,
            mutToSourseSinksPairs,
            taintCandidates,
            encounteredMethods,
            currentUtContext,
            mockAlwaysDefaults,
            classPath1,
            dependencyPaths,
            totalTimeoutMs
        )

        executionsByExecutable.forEach { (executable, executions) ->
            val taints = mutableListOf<ConfirmedTaint>()
            for (execution in executions) {
                // TODO should it be implicitly or explicitly thrown exception?
                if (execution.result !is UtExecutionFailure) {
                    continue
                }

                if ((execution.result as UtExecutionFailure).exception !is TaintAnalysisError) {
                    continue
                }

                val taintError = (execution.result as UtExecutionFailure).exception as TaintAnalysisError

                val path = execution.fullPath.map { it.stmt }
                val sink = taintError.taintSink
                val source = retrieveSource(path, immutableMutToSourseSinksPairs[executable]!!, sink) ?: continue

                val confirmedTaint = ConfirmedTaint(
                    source = source,
                    sink = sink,
                    sinkPosition = taintError.sinkSourcePosition,
                    path = retrievePath(path, source, sink)
                )

                taints += confirmedTaint
            }

            val analysisStopReason = analysisStopReasons[executable]
                ?: AnalysisStopReason.ERRORS(timeouts[executable]!!)
            executableTaints[executable] = UtBotExecutableTaints(taints, analysisStopReason)
        }

        return executableTaints
    }

    // Old version, does not respect timeout
    private fun runTaintAnalysisWithOneController(
        sortedByPriorityTaintMethods: List<TaintMethodWithTimeout>,
        mutToSourseSinksPairs: Map<ExecutableId, Map<Stmt, MutableSet<Stmt>>>,
        classLoader: URLClassLoader,
        classpathForEngine: String,
        dependencyPaths: String,
        mockAlwaysDefaults: MutableSet<ClassId>,
        confirmedTaints: MutableList<ConfirmedTaint>
    ) {
        disableCoroutinesDebug()

        sortedByPriorityTaintMethods.forEach { (method, taintPairs, timeoutMs) ->
            withUtContext(UtContext(classLoader)) {
                val executions = mutableListOf<UtExecution>()
                runBlocking {
                    val timeEstimator = ExecutionTimeEstimator(timeoutMs, methodsUnderTestNumber = 1)

                    //                val pathSelector = createPathSelector(method, taintPairs, timeoutMs)
                    // TODO pass it to the engine

                    val controller = EngineController()

                    controller.job = launch {

                        val engine = UtBotSymbolicEngine(
                            controller,
                            method,
                            classpathForEngine,
                            dependencyPaths = dependencyPaths,
                            mockStrategy = mockStrategy.toModel(),
                            chosenClassesToMockAlways = mockAlwaysDefaults,
                            solverTimeoutInMillis = timeEstimator.updatedSolverCheckTimeoutMillis
                        )

                        val resultFlow = defaultTestFlow(engine, timeEstimator.userTimeout)

                        resultFlow.collect {
                            when (it) {
                                is UtSymbolicExecution -> {
                                    executions += it
                                }
                                else -> logger.error { "Failed to analyze for taint: $it" }
                            }
                        }
                    }
                }

                logger.warn { "Method: $method, results: $executions" }

                val taintExecutions = executions
                    .filterIsInstance<UtSymbolicExecution>()
                    .filter {
                        it.result is UtExecutionFailure && (it.result as UtExecutionFailure).exception is TaintAnalysisError

                    }

                 taintExecutions.forEach executionLabel@{ execution ->
                     val taintError = (execution.result as UtExecutionFailure).exception as TaintAnalysisError

                     val path = execution.fullPath.map { it.stmt }
                     val sink = taintError.taintSink
                     val source = retrieveSource(path, mutToSourseSinksPairs[method]!!, sink) ?: return@executionLabel

                     val confirmedTaint = ConfirmedTaint(
                         source = source,
                         sink = sink,
                         sinkPosition = taintError.sinkSourcePosition,
                         path = retrievePath(path, source, sink)
                     )

                     confirmedTaints += confirmedTaint
                 }
            }
        }
    }

    private fun disableCoroutinesDebug() {
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_OFF)
    }

    private fun retrieveClassPaths(): Pair<String, String> {
        // TODO it is incorrect class path
        return "" to ""
    }

    private fun retrieveSource(path: List<Stmt>, sourcesToSink: Map<Stmt, Set<Stmt>>, sink: Stmt): Stmt? {
        // TODO can we really take any of these?
        val suitableSources = sourcesToSink.filter { sink in it.value }
        val result = path.firstOrNull { it in suitableSources.keys }

        if (result == null) {
            if (UtSettings.logDroppedTaintStates) {
                logger.warn {
                    "UtBot found unexpected taint error on instruction $sink ignored due to filtering mode"
                }
            }
        }

        return result
    }

    private fun retrievePath(
        path: List<Stmt>,
        source: Stmt,
        sink: Stmt
    ): List<Stmt> {
        val sourceIndex = path.indexOf(source)
        val sinkIndex = path.indexOf(sink)

        if (sourceIndex == -1 || sinkIndex == -1) {
            return path
        }

        return path.slice(sourceIndex..sinkIndex)
    }

    private fun createPathSelector(
        method: ExecutableId,
        taintPairs: TaintPairs,
        timeoutMs: Long
    ): Any {
        TODO("Not yet implemented")
    }
}

// TODO looks like bad design
val taintAnalysis: TaintAnalysis = TaintAnalysis()

data class TaintMethodWithTimeout(
    val method: ExecutableId, // TODO can it be a constructor?
    val taintPairs: TaintPairs,
    val timeoutMs: Long
)

data class ConfirmedTaint(
    val source: Stmt,
    val sink: Stmt,
    val sinkPosition: Int?,
    val path: TaintPath
)

class UtBotExecutableTaints(
    val taints: List<ConfirmedTaint>,
    val analysisStopReason: UtBotTaintAnalysis.AnalysisStopReason
)
