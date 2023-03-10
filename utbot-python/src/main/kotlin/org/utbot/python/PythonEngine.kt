package org.utbot.python

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.evaluation.*
import org.utbot.python.evaluation.serialiation.MemoryDump
import org.utbot.python.evaluation.serialiation.toPythonTree
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.fuzzing.*
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket

private val logger = KotlinLogging.logger {}

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<PythonFuzzedConcreteValue>,
    private val timeoutForRun: Long,
    private val pythonTypeStorage: PythonTypeStorage,
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
            is UtExplicitlyThrownException -> "${description.name}_with_exception"
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

    private fun handleSuccessResult(
        arguments: List<PythonFuzzedValue>,
        types: List<Type>,
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
                UtExplicitlyThrownException(Throwable(resultModel.type.toString()), false)
            }
            else {
                UtExecutionSuccess(PythonTreeModel(resultModel))
            }

        val testMethodName = suggestExecutionName(methodUnderTestDescription, executionResult)

        val (beforeThisObject, beforeModelList) = transformModelList(hasThisObject, evaluationResult.stateBefore, evaluationResult.modelListIds)
        val (afterThisObject, afterModelList) = transformModelList(hasThisObject, evaluationResult.stateAfter, evaluationResult.modelListIds)

        val utFuzzedExecution = UtFuzzedExecution(
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap()),
            stateAfter = EnvironmentModels(afterThisObject, afterModelList, emptyMap()),
            result = executionResult,
            coverage = evaluationResult.coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) }
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

    fun fuzzing(parameters: List<Type>, isCancelled: () -> Boolean, until: Long): Flow<FuzzingExecutionFeedback> = flow {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        ServerSocket(0).use { serverSocket ->
            logger.info { "Server port: ${serverSocket.localPort}" }
            val manager = try {
                PythonWorkerManager(
                    serverSocket,
                    pythonPath,
                    until,
                    { constructEvaluationInput(it) },
                    timeoutForRun.toInt()
                )
            } catch (_: TimeoutException) {
                return@flow
            }
            logger.info { "Executor manager was created successfully" }

            fun fuzzingResultHandler(
                description: PythonMethodDescription,
                arguments: List<PythonFuzzedValue>
            ): PythonExecutionResult? {
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
                    return when (val evaluationResult = manager.run(functionArguments, localAdditionalModules)) {
                        is PythonEvaluationError -> {
                            val utError = UtError(
                                "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}",
                                Throwable(evaluationResult.stackTrace.joinToString("\n"))
                            )
                            logger.debug(evaluationResult.stackTrace.joinToString("\n"))
                            PythonExecutionResult(InvalidExecution(utError), PythonFeedback(control = Control.PASS))
                        }

                        is PythonEvaluationTimeout -> {
                            val utError = UtError(evaluationResult.message, Throwable())
                            PythonExecutionResult(InvalidExecution(utError), PythonFeedback(control = Control.PASS))
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

                                is ArgumentsTypeErrorFeedback, is TypeErrorFeedback -> {
                                    PythonExecutionResult(result, PythonFeedback(control = Control.PASS))
                                }

                                is InvalidExecution -> {
                                    PythonExecutionResult(result, PythonFeedback(control = Control.CONTINUE))
                                }
                            }
                        }
                    }
                } catch (_: TimeoutException) {
                    logger.info { "Fuzzing process was interrupted by timeout" }
                    return null
                }
            }

            val pmd = PythonMethodDescription(
                methodUnderTest.name,
                parameters,
                fuzzedConcreteValues,
                pythonTypeStorage,
                Trie(Instruction::id)
            )

            if (parameters.isEmpty()) {
                val result = fuzzingResultHandler(pmd, emptyList())
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
                            emit(InvalidExecution(UtError("Bad input object", Throwable())))
                            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                        }

                        val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                        val mem = cache.get(pair)
                        if (mem != null) {
                            logger.debug("Repeat in fuzzing")
                            emit(mem.fuzzingExecutionFeedback)
                            return@PythonFuzzing mem.fuzzingPlatformFeedback
                        }
                        val result = fuzzingResultHandler(description, arguments)
                        if (result == null) {  // timeout
                            logger.info { "Fuzzing process was interrupted by timeout" }
                            manager.disconnect()
                            return@PythonFuzzing PythonFeedback(control = Control.STOP)
                        }

                        cache.add(pair, result)
                        emit(result.fuzzingExecutionFeedback)
                        return@PythonFuzzing result.fuzzingPlatformFeedback
                    }.fuzz(pmd)
                } catch (_: Exception) { // e.g. NoSeedValueException
                    logger.info { "Cannot fuzz values for types: ${parameters.map { it.pythonTypeRepresentation() }}" }
                }
            }
            manager.disconnect()
        }
    }
}
