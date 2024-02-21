package org.utbot.python.engine.fuzzing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtError
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.NoSeedValueException
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.CoverageIdGenerator
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.engine.CachedExecutionFeedback
import org.utbot.python.engine.ExecutionFeedback
import org.utbot.python.engine.ExecutionResultHandler
import org.utbot.python.engine.ExecutionStorage
import org.utbot.python.engine.FakeNodeFeedback
import org.utbot.python.engine.InvalidExecution
import org.utbot.python.engine.ValidExecution
import org.utbot.python.engine.fuzzing.typeinference.createMethodAnnotationModifications
import org.utbot.python.evaluation.EvaluationCache
import org.utbot.python.evaluation.PythonEvaluationError
import org.utbot.python.evaluation.PythonEvaluationSuccess
import org.utbot.python.evaluation.PythonEvaluationTimeout
import org.utbot.python.evaluation.PythonWorkerManager
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.PythonExecutionResult
import org.utbot.python.fuzzing.PythonFeedback
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonFuzzing
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utpython.types.PythonTypeHintsStorage
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utpython.types.general.FunctionType
import org.utpython.types.general.UtType
import org.utbot.python.newtyping.inference.InvalidTypeFeedback
import org.utbot.python.newtyping.inference.SuccessFeedback
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.inference.baseline.MethodAndVars
import org.utpython.types.mypy.MypyInfoBuild
import org.utpython.types.mypy.MypyReportLine
import org.utpython.types.mypy.getErrorNumber
import org.utpython.types.pythonModules
import org.utpython.types.pythonTypeName
import org.utpython.types.pythonTypeRepresentation
import org.utpython.types.utils.getOffsetLine
import org.utbot.python.utils.ExecutionWithTimeoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import org.utbot.python.utils.TimeoutMode
import org.utbot.python.utils.convertToTime
import java.net.ServerSocket
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private const val RANDOM_TYPE_FREQUENCY = 4
private const val MINIMAL_TIMEOUT_FOR_SUBSTITUTION = 4_000  // ms

class FuzzingEngine(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    private val typeStorage: PythonTypeHintsStorage,
    private val hintCollector: HintCollector,
    constantCollector: ConstantCollector,
    private val mypyStorage: MypyInfoBuild,
    private val mypyReport: List<MypyReportLine>,
    val until: Long,
    private val executionStorage: ExecutionStorage,
) {
    private val cache = EvaluationCache()

    private val constants: List<PythonFuzzedConcreteValue> = constantCollector.result
        .mapNotNull { (type, value) ->
            if (type.pythonTypeName() == pythonStrClassId.name && value is String) {
                // Filter doctests
                if (value.contains(">>>")) return@mapNotNull null
            }
            logger.debug { "Collected constant: ${type.pythonTypeRepresentation()}: $value" }
            PythonFuzzedConcreteValue(type, value)
        }

    fun start() {
        logger.info { "Fuzzing until: ${until.convertToTime()}" }
        val modifications = createMethodAnnotationModifications(method, typeStorage)
        val now = System.currentTimeMillis()
        val filterModifications = modifications
            .take(minOf(modifications.size, maxOf(((until - now) / MINIMAL_TIMEOUT_FOR_SUBSTITUTION).toInt(), 1)))
            .map { (modifiedMethod, additionalVars) ->
                logger.info { "Substitution: ${modifiedMethod.methodSignature()}" }
                MethodAndVars(modifiedMethod, additionalVars)
            }
        generateTests(method, filterModifications, until)
    }

    private fun generateTests(
        method: PythonMethod,
        methodModifications: List<MethodAndVars>,
        until: Long,
    ) {
        val timeoutLimitManager = TestGenerationLimitManager(
            TimeoutMode,
            until,
        )
        val namesInModule = mypyStorage.names
            .getOrDefault(configuration.testFileInformation.moduleName, emptyList())
            .map { it.name }
            .filter {
                it.length < 4 || !it.startsWith("__") || !it.endsWith("__")
            }

        val sourceFileContent = configuration.testFileInformation.testedFileContent
        val algo = BaselineAlgorithm(
            typeStorage,
            hintCollector.result,
            configuration.pythonPath,
            MethodAndVars(method, ""),
            methodModifications,
            configuration.sysPathDirectories,
            configuration.testFileInformation.moduleName,
            namesInModule,
            getErrorNumber(
                mypyReport,
                configuration.testFileInformation.testedFilePath,
                getOffsetLine(sourceFileContent, method.ast.beginOffset),
                getOffsetLine(sourceFileContent, method.ast.endOffset)
            ),
            mypyStorage.buildRoot.configFile,
            randomTypeFrequency = RANDOM_TYPE_FREQUENCY,
            dMypyTimeout = configuration.timeoutForRun,
        )

        val fuzzerCancellation = { configuration.isCanceled() || timeoutLimitManager.isCancelled() }
        runBlocking {
            runFuzzing(
                method,
                algo,
                fuzzerCancellation,
                until
            ).collect {
                executionStorage.saveFuzzingExecution(it)
            }
        }
    }

    private fun runFuzzing(
        method: PythonMethod,
        typeInferenceAlgorithm: BaselineAlgorithm,
        isCancelled: () -> Boolean,
        until: Long
    ): Flow<ExecutionFeedback> = flow {
        ServerSocket(0).use { serverSocket ->
            logger.debug { "Server port: ${serverSocket.localPort}" }
            val manager = try {
                PythonWorkerManager(
                    method,
                    serverSocket,
                    configuration,
                    until,
                )
            } catch (_: TimeoutException) {
                return@use
            }
            logger.debug { "Executor manager was created successfully" }

            val initialType = (typeInferenceAlgorithm.expandState() ?: method.methodType) as FunctionType

            val pmd = PythonMethodDescription(
                method.name,
                constants,
                typeStorage,
                Trie(PyInstruction::id),
                Random(0),
                TestGenerationLimitManager(ExecutionWithTimeoutMode, until, isRootManager = true),
                initialType,
            )

            try {
                val parameters = method.methodType.arguments
                if (parameters.isEmpty()) {
                    val result = fuzzingResultHandler(pmd, emptyList(), parameters, manager)
                    result?.let {
                        emit(it.executionFeedback)
                    }
                } else {
                    try {
                        PythonFuzzing(typeStorage, typeInferenceAlgorithm, isCancelled) { description, arguments ->
                            if (isCancelled()) {
                                logger.debug { "Fuzzing process was interrupted" }
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }
                            if (System.currentTimeMillis() >= until) {
                                logger.debug { "Fuzzing process was interrupted by timeout" }
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }

                            if (arguments.any { PythonTree.containsFakeNode(it.tree) }) {
                                logger.debug { "FakeNode in Python model" }
                                description.limitManager.addFakeNodeExecutions()
                                emit(FakeNodeFeedback)
                                return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                            } else {
                                description.limitManager.restartFakeNode()
                            }

                            val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                            val mem = cache.get(pair)
                            if (mem != null) {
                                logger.debug { "Repeat in fuzzing ${arguments.map { it.tree }}" }
                                description.limitManager.addSuccessExecution()
                                emit(CachedExecutionFeedback(mem.executionFeedback))
                                return@PythonFuzzing mem.fuzzingPlatformFeedback.fromCache()
                            }
                            val result = fuzzingResultHandler(description, arguments, parameters, manager)
                            if (result == null) {  // timeout
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }

                            cache.add(pair, result)
                            emit(result.executionFeedback)
                            return@PythonFuzzing result.fuzzingPlatformFeedback
                        }.fuzz(pmd)
                    } catch (_: NoSeedValueException) {
                        logger.debug { "Cannot fuzz values for types: ${parameters.map { it.pythonTypeRepresentation() }}" }
                    }
                }
            } finally {
                manager.shutdown()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun handleTimeoutResult(
        arguments: List<PythonFuzzedValue>,
        coveredInstructions: List<PyInstruction>,
    ): ExecutionFeedback {
        val summary = arguments
            .zip(method.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

        return ExecutionResultHandler.handleTimeoutResult(
            method,
            arguments.map { PythonTreeModel(it.tree) },
            coveredInstructions,
            summary.map { DocRegularStmt(it) }
        )
    }

    private fun handleSuccessResult(
        arguments: List<PythonFuzzedValue>,
        types: List<UtType>,
        evaluationResult: PythonEvaluationSuccess,
    ): ExecutionFeedback {
        val summary = arguments
            .zip(method.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

        return ExecutionResultHandler.handleSuccessResult(
            method,
            configuration,
            types,
            evaluationResult,
            summary.map { DocRegularStmt(it) },
        )
    }

    private fun fuzzingResultHandler(
        description: PythonMethodDescription,
        arguments: List<PythonFuzzedValue>,
        parameters: List<UtType>,
        manager: PythonWorkerManager,
    ): PythonExecutionResult? {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        val argumentValues = arguments.map { PythonTreeModel(it.tree, it.tree.type) }
        val moduleToImport = configuration.testFileInformation.moduleName
        val argumentModules = argumentValues
            .flatMap { it.allContainingClassIds }
            .map { it.moduleName }
            .filterNot { it.startsWith(moduleToImport) }
        val localAdditionalModules = (additionalModules + argumentModules + moduleToImport).toSet()

        val (thisObject, modelList) = if (method.hasThisArgument)
            Pair(argumentValues[0], argumentValues.drop(1))
        else
            Pair(null, argumentValues)
        val functionArguments = FunctionArguments(
            thisObject,
            method.thisObjectName,
            modelList,
            method.argumentsNamesWithoutSelf
        )
        try {
            val coverageId = CoverageIdGenerator.createId()
            return when (val evaluationResult =
                manager.runWithCoverage(functionArguments, localAdditionalModules, coverageId)) {
                is PythonEvaluationError -> {
                    val stackTraceMessage = evaluationResult.stackTrace.joinToString("\n")
                    val utError = UtError(
                        "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}\n${stackTraceMessage}",
                        Throwable(stackTraceMessage)
                    )
                    description.limitManager.addInvalidExecution()
                    logger.debug(stackTraceMessage)
                    PythonExecutionResult(InvalidExecution(utError), PythonFeedback(control = Control.PASS))
                }

                is PythonEvaluationTimeout -> {
                    val coveredInstructions =
                        manager.coverageReceiver.coverageStorage.getOrDefault(coverageId, mutableListOf())
                    val utTimeoutException = handleTimeoutResult(arguments, coveredInstructions)
                    val trieNode: Trie.Node<PyInstruction> =
                        if (coveredInstructions.isEmpty())
                            Trie.emptyNode()
                        else
                            description.tracer.add(coveredInstructions)
                    description.limitManager.addInvalidExecution()
                    PythonExecutionResult(
                        utTimeoutException,
                        PythonFeedback(control = Control.PASS, result = trieNode, SuccessFeedback)
                    )
                }

                is PythonEvaluationSuccess -> {
                    val coveredInstructions = evaluationResult.coveredStatements

                    val result = handleSuccessResult(
                        arguments,
                        parameters,
                        evaluationResult,
                    )
                    val typeInferenceFeedback = if (result is ValidExecution) SuccessFeedback else InvalidTypeFeedback
                    when (result) {
                        is ValidExecution -> {
                            val trieNode: Trie.Node<PyInstruction> = description.tracer.add(coveredInstructions)
                            description.limitManager.addSuccessExecution()
                            PythonExecutionResult(
                                result,
                                PythonFeedback(Control.CONTINUE, trieNode, typeInferenceFeedback)
                            )
                        }
                        is InvalidExecution -> {
                            description.limitManager.addInvalidExecution()
                            PythonExecutionResult(result, PythonFeedback(control = Control.CONTINUE, typeInferenceFeedback = typeInferenceFeedback))
                        }
                        else -> {
                            description.limitManager.addInvalidExecution()
                            PythonExecutionResult(result, PythonFeedback(control = Control.PASS, typeInferenceFeedback = typeInferenceFeedback))
                        }
                    }
                }
            }
        } catch (_: TimeoutException) {
            logger.debug { "Fuzzing process was interrupted by timeout" }
            return null
        }
    }
}