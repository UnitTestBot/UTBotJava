package org.utbot.engine.taint

import com.jetbrains.rd.util.concurrentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.common.runIgnoringCancellationException
import org.utbot.engine.EngineController
import org.utbot.engine.Mocker
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.logger
import org.utbot.engine.taint.priority.SimpleTaintMethodsAnalysisPrioritizer
import org.utbot.engine.taint.priority.TaintMethodsAnalysisPrioritizer
import org.utbot.engine.taint.timeout.SimpleTaintTimeoutStrategy
import org.utbot.engine.taint.timeout.TaintTimeoutStrategy
import org.utbot.framework.AnalysisMode
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator.ExecutionTimeEstimator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.defaultTestFlow
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.util.toModel
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
    private val mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS

    init {
        passTaintAnalysisConfiguration()
    }

    private fun passTaintAnalysisConfiguration() {
        // TODO use configuration in TaintAnalysis
    }

    // TODO copy-paste from org.utbot.cli.util.ClassLoaderUtilsKt.toUrl
    private fun String.toUrl(): URL = File(this).toURI().toURL()

    // TODO copy-paste from org.utbot.cli.util.ClassLoaderUtilsKt.createClassLoader
    private fun createClassLoader(classPath: String? = "", absoluteFileNameWithClasses: String? = null): URLClassLoader {
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
        class STEPS(elapsedTimeMs: Long) : AnalysisStopReason(elapsedTimeMs, explanation = "Step limit ${UtSettings.pathSelectorStepsLimit} exceeding")
        class ERRORS(elapsedTimeMs: Long) : AnalysisStopReason(elapsedTimeMs, explanation = "Errors during analysis occurred")
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
        currentUtContext: UtContext,
        mockAlwaysDefaults: Set<ClassId>,
        classPath: String,
        dependencyPaths: String,
        totalTimeoutMs: Long
    ): Pair<MutableMap<ExecutableId, MutableList<UtSymbolicExecution>>, MutableMap<ExecutableId, AnalysisStopReason>> {
        disableCoroutinesDebug()

        val executionStartInMillis = System.currentTimeMillis()

        val method2controller = sortedByPriorityTaintMethodsWithTimeouts.associateWith {
            ControllerWithTimeEstimator(
                EngineController(),
                TaintTimeEstimator(it.timeoutMs)
            )
        }
        val isCanceled = { false }

        val executionsByExecutable = mutableMapOf<ExecutableId, MutableList<UtSymbolicExecution>>().apply {
            // Fill with empty lists of executions for logging later
            sortedByPriorityTaintMethodsWithTimeouts.forEach {
                put(it.method, mutableListOf())
            }
        }

        // Concurrent seems to be required here
        val analysisStopReasons = concurrentMapOf<ExecutableId, AnalysisStopReason>()

        runIgnoringCancellationException {
            runBlockingWithCancellationPredicate(isCanceled) {
                var extraTimeBudgetMs = 0L

                for ((methodWithTimeout, controllerWithTimeEstimator) in method2controller) {
                    val (controller, timeEstimator) = controllerWithTimeEstimator

                    controller.job = launch(currentUtContext) {
                        if (!isActive) return@launch

                        val method = methodWithTimeout.method
                        try {
                            //yield one to
                            yield()

                            // TODO create an appropriate path selector and pass it to the engine here

                            val engine = UtBotSymbolicEngine(
                                controller,
                                method,
                                classPath,
                                dependencyPaths,
                                mockStrategy = mockStrategy.toModel(),
                                chosenClassesToMockAlways = mockAlwaysDefaults,
                                solverTimeoutInMillis = getUpdatedSolverCheckTimeoutMs(methodWithTimeout.timeoutMs)
                            )

                            val resultFlow = defaultTestFlow(engine, timeEstimator.totalTimeBudgetMs)

                            resultFlow
                                .onCompletion {
                                    extraTimeBudgetMs += timeEstimator.remainingTimeBudgetMs

                                    if (it != null) {
                                        logger.error(it) { "Error in taint flow for $method" }
                                        return@onCompletion
                                    }

                                    if (engine.wasStoppedByStepsLimit) {
                                        analysisStopReasons.getOrPut(method) {
                                            logger.warn {
                                                "Taint analysis of the executable $method was stopped due to exceeding steps limit, elapsed time ${timeEstimator.elapsedTimeMs} (ms)"
                                            }

                                            AnalysisStopReason.STEPS(timeEstimator.elapsedTimeMs)
                                        }
                                    } else {
                                        analysisStopReasons.getOrPut(method) {
                                            logger.warn {
                                                "Taint analysis of the executable $method ended due to full coverage, elapsed time ${timeEstimator.elapsedTimeMs} (ms)"
                                            }

                                            AnalysisStopReason.COVERAGE(timeEstimator.elapsedTimeMs)
                                        }
                                    }
                                }
                                .catch {
                                    logger.error(it) { "Error in flow during taint analysis" }
                                }
                                .collect {
                                    when (it) {
                                        is UtSymbolicExecution -> {
                                            executionsByExecutable[method]?.add(it)
                                            logger.warn { "Method: ${method}, executions: $it" }
                                        }
                                        is UtError -> {
                                            analysisStopReasons.putIfAbsent(method, AnalysisStopReason.ERRORS(timeEstimator.elapsedTimeMs))
                                            logger.error(it.error) { "Failed to analyze for taint: $it" }
                                        }
                                        else -> logger.error { "Unexpected taint execution $it" }
                                }
                            }
                        } catch (e: CancellationException) {
                            // Do nothing
                        } catch (e: Exception) {
                            logger.error(e) { "Error in engine during taint analysis" }
                        }
                    }
                    controller.paused = true
                }

                GlobalScope.launch {
                    while (isActive) {
                        var activeCount = 0
                        for ((methodWithTimeout, controllerWithTimeEstimator) in method2controller) {
                            val (controller, timeEstimator) = controllerWithTimeEstimator
                            timeEstimator.setStart()
                            timeEstimator.addTimeBudget(extraTimeBudgetMs)
                            extraTimeBudgetMs = 0L

                            val job = controller.job

                            if (!job!!.isActive) {
                                continue
                            }
                            activeCount++

                            method2controller.values.forEach { it.controller.paused = true }
                            controller.paused = false

                            while (job.isActive && !timeEstimator.isTimeElapsed()) {
                                val timePassed = System.currentTimeMillis() - executionStartInMillis

                                if (timePassed > totalTimeoutMs) {
                                    method2controller.values.forEach { it.controller.job!!.cancel("Timeout") }
                                    cancel("Timeout")
                                    analysisStopReasons.keys.forEach { analysisStopReasons.putIfAbsent(it, AnalysisStopReason.TIMEOUT(timeEstimator.elapsedTimeMs)) }
                                    logger.warn { "Total timeout $totalTimeoutMs (ms) exceeded, analysis is canceled" }
                                }

                                yield()
                            }

                            if (job.isActive) {
                                val method = methodWithTimeout.method
                                analysisStopReasons[method] = AnalysisStopReason.TIMEOUT(timeEstimator.elapsedTimeMs)
                                logger.warn { "Analysis of executable $method was interrupted due to exceeding it's timeout ${timeEstimator.totalTimeBudgetMs}" }
                            }
                        }

                        // TODO do we really need this variable and this break?
                        if (activeCount == 0) {
                            logger.error { "No active analysis tasks, interrupted" }
                            break
                        }
                    }
                }
            }
        }

        executionsByExecutable.entries.forEach {
            logger.warn {
                "Method: ${it.key}, number of executions: ${it.value.size}, end of analysis reason: ${analysisStopReasons[it.key]}"
            }
        }

        return executionsByExecutable to analysisStopReasons
    }

    fun runTaintAnalysis(
        taintCandidates: TaintCandidates,
        totalTimeoutMs: Long,
        classPath: String
    ): MutableMap<ExecutableId, Pair<List<ConfirmedTaint>, AnalysisStopReason>> {
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
        AnalysisMode.TAINT.applyMode()
        // TODO move to the TAINT analysis mode?
        UtSettings.useOnlyTaintAnalysis = true

        val mockAlwaysDefaults = Mocker.javaDefaultClassesToMockAlways.mapTo(mutableSetOf()) { it.id }
        val (classpathForEngine, dependencyPaths) = retrieveClassPaths()
        val executableTaints = mutableMapOf<ExecutableId, Pair<List<ConfirmedTaint>, AnalysisStopReason>>()

        taintAnalysis.setConfiguration(taintConfiguration)

        val classPath1 = "$classPath${File.pathSeparatorChar}${System.getProperty("java.class.path")}"
        val classLoader = createClassLoader(classPath1)

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
            currentUtContext,
            mockAlwaysDefaults,
            classPath1,
            dependencyPaths,
            totalTimeoutMs
        )

        executionsByExecutable.forEach { (executable, executions) ->
            val taints = mutableListOf<ConfirmedTaint>()
            for (execution in executions) {
                if (execution.result !is UtExplicitlyThrownException) {
                    continue
                }
                if ((execution.result as UtExplicitlyThrownException).exception !is TaintAnalysisError) {
                    continue
                }

                val taintError = (execution.result as UtExplicitlyThrownException).exception as TaintAnalysisError

                val path = execution.fullPath.map { it.stmt }
                val source = retrieveSource(path, taintCandidates[executable]!!.keys.mapTo(mutableSetOf()) { it.stmt })
                val sink = taintError.taintSink

                val confirmedTaint = ConfirmedTaint(
                    source = source,
                    sink = sink,
                    sinkPosition = taintError.sinkSourcePosition,
                    path = retrievePath(path, source, sink)
                )

                taints += confirmedTaint
            }

            executableTaints[executable] = taints to (analysisStopReasons[executable] ?: AnalysisStopReason.ERRORS(timeouts[executable]!!))
        }

        return executableTaints
    }

    // Old version, does not respect timeout
    private fun runTaintAnalysisWithOneController(
        sortedByPriorityTaintMethods: List<TaintMethodWithTimeout>,
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
                        it.result is UtExplicitlyThrownException && (it.result as UtExplicitlyThrownException).exception is TaintAnalysisError

                    }

                taintExecutions.forEach { execution ->
                    val taintError = (execution.result as UtExplicitlyThrownException).exception as TaintAnalysisError

                    val path = execution.fullPath.map { it.stmt }
                    val source = retrieveSource(path, taintPairs.keys.mapTo(mutableSetOf()) { it.stmt })
                    val sink = taintError.taintSink

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

    private fun retrieveSource(path: List<Stmt>, sources: Set<Stmt>): Stmt =
        // TODO can we really take any of these?
        path.first { it in sources }

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
