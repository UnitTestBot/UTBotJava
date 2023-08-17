package org.utbot.python

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.NoSeedValueException
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.evaluation.*
import org.utbot.python.evaluation.serialiation.MemoryDump
import org.utbot.python.evaluation.serialiation.toPythonTree
import org.utbot.python.evaluation.utils.CoverageIdGenerator
import org.utbot.python.evaluation.utils.coveredLinesToInstructions
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.fuzzing.*
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<PythonFuzzedConcreteValue>,
    private val timeoutForRun: Long,
    private val pythonTypeStorage: PythonTypeHintsStorage,
) {

    private val cache = EvaluationCache()

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

    private fun transformModelList(
        hasThisObject: Boolean,
        state: MemoryDump,
        modelListIds: List<String>
    ): Pair<UtModel?, List<UtModel>> {
        val (stateThisId, resultModelListIds) =
            if (hasThisObject) {
                Pair(modelListIds.first(), modelListIds.drop(1))
            } else {
                Pair(null, modelListIds)
            }
        val stateThisObject = stateThisId?.let {
            PythonTreeModel(
                state.getById(it).toPythonTree(state)
            )
        }
        val modelList = resultModelListIds.map {
            PythonTreeModel(
                state.getById(it).toPythonTree(state)
            )
        }
        return Pair(stateThisObject, modelList)
    }

    private fun handleTimeoutResult(
        arguments: List<PythonFuzzedValue>,
        methodUnderTestDescription: PythonMethodDescription,
        coveredLines: Collection<Int>,
    ): FuzzingExecutionFeedback {
        val summary = arguments
            .zip(methodUnderTest.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }
        val executionResult = UtTimeoutException(TimeoutException("Execution is too long"))
        val testMethodName = suggestExecutionName(methodUnderTestDescription, executionResult)

        val hasThisObject = methodUnderTest.hasThisArgument
        val (beforeThisObjectTree, beforeModelListTree) = if (hasThisObject) {
            arguments.first() to arguments.drop(1)
        } else {
            null to arguments
        }
        val beforeThisObject = beforeThisObjectTree?.let { PythonTreeModel(it.tree) }
        val beforeModelList = beforeModelListTree.map { PythonTreeModel(it.tree) }

        val coveredInstructions = coveredLinesToInstructions(coveredLines, methodUnderTest)
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
            arguments = methodUnderTest.argumentsWithoutSelf
        )
        return ValidExecution(utFuzzedExecution)
    }
    private fun handleSuccessResult(
        arguments: List<PythonFuzzedValue>,
        types: List<UtType>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: PythonMethodDescription,
    ): FuzzingExecutionFeedback {
        val prohibitedExceptions = listOf(
            "builtins.AttributeError",
            "builtins.TypeError"
        )

        val summary = arguments
            .zip(methodUnderTest.arguments)
            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }
        val hasThisObject = methodUnderTest.hasThisArgument

        val resultModel = evaluationResult.stateAfter.getById(evaluationResult.resultId).toPythonTree(evaluationResult.stateAfter)

        if (evaluationResult.isException && (resultModel.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.pythonTypeRepresentation() }
            }. Exception type: ${resultModel.type.name}"

            logger.info(errorMessage)
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
            coverage = evaluationResult.coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) },
            arguments = methodUnderTest.argumentsWithoutSelf,
        )
        return ValidExecution(utFuzzedExecution)
    }

    private fun constructEvaluationInput(pythonWorker: PythonWorker): PythonCodeExecutor {
        return PythonCodeSocketExecutor(
            methodUnderTest,
            moduleToImport,
            pythonPath,
            directoriesForSysPath,
            timeoutForRun,
            pythonWorker,
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
        logger.debug(argumentValues.map { it.tree } .toString())
        val argumentModules = argumentValues
            .flatMap { it.allContainingClassIds }
            .map { it.moduleName }
            .filterNot { it.startsWith(moduleToImport) }
        val localAdditionalModules = (additionalModules + argumentModules + moduleToImport).toSet()

        val (thisObject, modelList) =
            if (methodUnderTest.hasThisArgument)
                Pair(argumentValues[0], argumentValues.drop(1))
            else
                Pair(null, argumentValues)
        val functionArguments = FunctionArguments(
            thisObject,
            methodUnderTest.thisObjectName,
            modelList,
            methodUnderTest.argumentsNames
        )
        try {
            val coverageId = CoverageIdGenerator.createId()
            return when (val evaluationResult =
                manager.runWithCoverage(functionArguments, localAdditionalModules, coverageId)) {
                is PythonEvaluationError -> {
                    val utError = UtError(
                        "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}",
                        Throwable(evaluationResult.stackTrace.joinToString("\n"))
                    )
                    logger.debug(evaluationResult.stackTrace.joinToString("\n"))
                    PythonExecutionResult(InvalidExecution(utError), PythonFeedback(control = Control.PASS))
                }

                is PythonEvaluationTimeout -> {
                    val coveredLines =
                        manager.coverageReceiver.coverageStorage.getOrDefault(coverageId, mutableSetOf())
                    val utTimeoutException = handleTimeoutResult(arguments, description, coveredLines)
                    val coveredInstructions = coveredLinesToInstructions(coveredLines, methodUnderTest)
                    val trieNode: Trie.Node<Instruction> =
                        if (coveredInstructions.isEmpty())
                            Trie.emptyNode()
                        else
                            description.tracer.add(coveredInstructions)
                    PythonExecutionResult(
                        utTimeoutException,
                        PythonFeedback(control = Control.PASS, result = trieNode)
                    )
                }

                is PythonEvaluationSuccess -> {
                    val coveredInstructions = evaluationResult.coverage.coveredInstructions

                    when (val result = handleSuccessResult(
                        arguments,
                        parameters,
                        evaluationResult,
                        description,
                    )) {
                        is ValidExecution -> {
                            val trieNode: Trie.Node<Instruction> = description.tracer.add(coveredInstructions)
                            PythonExecutionResult(
                                result,
                                PythonFeedback(control = Control.CONTINUE, result = trieNode)
                            )
                        }
                        is InvalidExecution -> {
                            PythonExecutionResult(result, PythonFeedback(control = Control.CONTINUE))
                        }
                        else -> {
                            PythonExecutionResult(result, PythonFeedback(control = Control.PASS))
                        }
                    }
                }
            }
        } catch (_: TimeoutException) {
            logger.info { "Fuzzing process was interrupted by timeout" }
            return null
        }
    }

    fun fuzzing(parameters: List<UtType>, isCancelled: () -> Boolean, until: Long): Flow<FuzzingExecutionFeedback> = flow {
        ServerSocket(0).use { serverSocket ->
            logger.info { "Server port: ${serverSocket.localPort}" }
            val manager = try {
                PythonWorkerManager(
                    serverSocket,
                    pythonPath,
                    until,
                ) { constructEvaluationInput(it) }
            } catch (_: TimeoutException) {
                return@flow
            }
            logger.info { "Executor manager was created successfully" }

            val pmd = PythonMethodDescription(
                methodUnderTest.name,
                parameters,
                fuzzedConcreteValues,
                pythonTypeStorage,
                Trie(Instruction::id),
                Random(0),
            )

            try {
                if (parameters.isEmpty()) {
                    val result = fuzzingResultHandler(pmd, emptyList(), parameters, manager)
                    result?.let {
                        emit(it.fuzzingExecutionFeedback)
                    }
                } else {
                    try {
                        PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
                            if (isCancelled()) {
                                logger.info { "Fuzzing process was interrupted" }
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }
                            if (System.currentTimeMillis() >= until) {
                                logger.info { "Fuzzing process was interrupted by timeout" }
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }

                            if (arguments.any { PythonTree.containsFakeNode(it.tree) }) {
                                logger.debug("FakeNode in Python model")
                                emit(FakeNodeFeedback)
                                return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                            }

                            val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                            val mem = cache.get(pair)
                            if (mem != null) {
                                logger.debug("Repeat in fuzzing")
                                emit(CachedExecutionFeedback(mem.fuzzingExecutionFeedback))
                                return@PythonFuzzing mem.fuzzingPlatformFeedback
                            }
                            val result = fuzzingResultHandler(description, arguments, parameters, manager)
                            if (result == null) {  // timeout
                                manager.disconnect()
                                return@PythonFuzzing PythonFeedback(control = Control.STOP)
                            }

                            cache.add(pair, result)
                            emit(result.fuzzingExecutionFeedback)
                            return@PythonFuzzing result.fuzzingPlatformFeedback
                        }.fuzz(pmd)
                    } catch (_: NoSeedValueException) {
                        logger.info { "Cannot fuzz values for types: ${parameters.map { it.pythonTypeRepresentation() }}" }
                    }
                }
            } finally {
                manager.shutdown()
            }
        }
    }
}
