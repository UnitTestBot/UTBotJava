package org.utbot.python.engine.fuzzing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.NoSeedValueException
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.CoverageIdGenerator
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.coverage.buildCoverage
import org.utbot.python.engine.CachedExecutionFeedback
import org.utbot.python.engine.ExecutionFeedback
import org.utbot.python.engine.ExecutionStorage
import org.utbot.python.engine.FakeNodeFeedback
import org.utbot.python.engine.InvalidExecution
import org.utbot.python.engine.TypeErrorFeedback
import org.utbot.python.engine.ValidExecution
import org.utbot.python.engine.fuzzing.typeinference.createMethodAnnotationModifications
import org.utbot.python.engine.utils.transformModelList
import org.utbot.python.evaluation.EvaluationCache
import org.utbot.python.evaluation.PythonCodeSocketExecutor
import org.utbot.python.evaluation.PythonEvaluationError
import org.utbot.python.evaluation.PythonEvaluationSuccess
import org.utbot.python.evaluation.PythonEvaluationTimeout
import org.utbot.python.evaluation.PythonWorkerManager
import org.utbot.python.evaluation.serialization.toPythonTree
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.PythonExecutionResult
import org.utbot.python.fuzzing.PythonFeedback
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonFuzzing
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.InvalidTypeFeedback
import org.utbot.python.newtyping.inference.SuccessFeedback
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.getErrorNumber
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.newtyping.utils.getOffsetLine
import org.utbot.python.utils.ExecutionWithTimoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import org.utbot.python.utils.TimeoutMode
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.python.utils.convertToTime
import org.utbot.python.utils.separateUntil
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private const val RANDOM_TYPE_FREQUENCY = 6

class FuzzingEngine(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    val typeStorage: PythonTypeHintsStorage,
    val hintCollector: HintCollector,
    val constantCollector: ConstantCollector,
    val mypyStorage: MypyInfoBuild,
    val mypyReport: List<MypyReportLine>,
    val until: Long,
    val executionStorage: ExecutionStorage,
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
        filterModifications
            .forEachIndexed { index, (modifiedMethod, additionalVars) ->
                logger.info { "Modified method: ${modifiedMethod.methodSignature()}" }
                val localUntil = separateUntil(until, index, filterModifications.size)
                logger.info { "Fuzzing local until: ${localUntil.convertToTime()}" }
                generateTests(modifiedMethod, additionalVars, localUntil)
            }
    }

    private fun generateTests(
        method: PythonMethod,
        additionalVars: String,
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
            method,
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
            additionalVars,
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
                        serverSocket,
                        configuration.pythonPath,
                        until,
                        configuration.coverageMeasureMode,
                        configuration.sendCoverageContinuously,
                    ) {
                        PythonCodeSocketExecutor(
                            method,
                            configuration.testFileInformation.moduleName,
                            configuration.pythonPath,
                            configuration.sysPathDirectories,
                            configuration.timeoutForRun,
                            it,
                        )
                    }
                } catch (_: TimeoutException) {
                    return@use
                }
                logger.debug { "Executor manager was created successfully" }

                val pmd = PythonMethodDescription(
                    method.name,
                    constants,
                    typeStorage,
                    Trie(PyInstruction::id),
                    Random(0),
                    TestGenerationLimitManager(ExecutionWithTimoutMode, until, isRootManager = true),
                    method.definition.type,
                )

                try {
                    val parameters = method.definition.type.arguments
                    if (parameters.isEmpty()) {
                        val result = fuzzingResultHandler(pmd, emptyList(), parameters, manager)
                        result?.let {
                            emit(it.executionFeedback)
                        }
                    } else {
                        try {
                            PythonFuzzing(typeStorage, typeInferenceAlgorithm) { description, arguments ->
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

    private fun suggestExecutionName(
        description: PythonMethodDescription,
        executionResult: UtExecutionResult
    ): TestSuggestedInfo {
        val testSuffix = when (executionResult) {
            is UtExecutionSuccess -> {
                // can be improved
                description.name
            }
            is UtExecutionFailure -> "${description.name}_with_exception"
            else -> description.name
        }
        val testName = "test_$testSuffix"
        return TestSuggestedInfo(
            testName,
            testName,
        )
    }

    private fun handleTimeoutResult(
        arguments: List<PythonFuzzedValue>,
        methodUnderTestDescription: PythonMethodDescription,
        coveredInstructions: List<PyInstruction>,
    ): ExecutionFeedback {
        val summary = arguments
            .zip(method.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }
        val executionResult = UtTimeoutException(TimeoutException("Execution is too long"))
        val testMethodName = suggestExecutionName(methodUnderTestDescription, executionResult)

        val hasThisObject = method.hasThisArgument
        val (beforeThisObjectTree, beforeModelListTree) = if (hasThisObject) {
            arguments.first() to arguments.drop(1)
        } else {
            null to arguments
        }
        val beforeThisObject = beforeThisObjectTree?.let { PythonTreeModel(it.tree) }
        val beforeModelList = beforeModelListTree.map { PythonTreeModel(it.tree) }

        val coverage = Coverage(coveredInstructions)
        val utFuzzedExecution = PythonUtExecution(
            stateInit = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateAfter = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            diffIds = emptyList(),
            result = executionResult,
            coverage = coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) },
            arguments = method.argumentsWithoutSelf
        )
        return ValidExecution(utFuzzedExecution)
    }

    private fun handleSuccessResult(
        arguments: List<PythonFuzzedValue>,
        types: List<UtType>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: PythonMethodDescription,
    ): ExecutionFeedback {
        val summary = arguments
            .zip(method.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }
        val hasThisObject = method.hasThisArgument

        val resultModel = evaluationResult.stateAfter.getById(evaluationResult.resultId).toPythonTree(evaluationResult.stateAfter)

        if (evaluationResult.isException && (resultModel.type.name in configuration.prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.pythonTypeRepresentation() }
            }. Exception type: ${resultModel.type.name}"

            logger.debug { errorMessage }
            return TypeErrorFeedback(errorMessage)
        }

        val executionResult =
            if (evaluationResult.isException) {
                UtImplicitlyThrownException(Throwable(resultModel.type.toString()), false)
            }
            else {
                UtExecutionSuccess(PythonTreeModel(resultModel))
            }

        val testMethodName = suggestExecutionName(methodUnderTestDescription, executionResult)

        val (thisObject, initModelList) = transformModelList(hasThisObject, evaluationResult.stateInit, evaluationResult.modelListIds)
        val (beforeThisObject, beforeModelList) = transformModelList(hasThisObject, evaluationResult.stateBefore, evaluationResult.modelListIds)
        val (afterThisObject, afterModelList) = transformModelList(hasThisObject, evaluationResult.stateAfter, evaluationResult.modelListIds)

        val utFuzzedExecution = PythonUtExecution(
            stateInit = EnvironmentModels(thisObject, initModelList, emptyMap(), executableToCall = null),
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateAfter = EnvironmentModels(afterThisObject, afterModelList, emptyMap(), executableToCall = null),
            diffIds = evaluationResult.diffIds,
            result = executionResult,
            coverage = buildCoverage(evaluationResult.coveredStatements, evaluationResult.missedStatements),
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) },
            arguments = method.argumentsWithoutSelf,
        )
        return ValidExecution(utFuzzedExecution)
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
            method.argumentsNames
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
                    val utTimeoutException = handleTimeoutResult(arguments, description, coveredInstructions)
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
                        description,
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