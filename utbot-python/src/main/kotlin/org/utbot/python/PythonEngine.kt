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
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.fuzzing.*
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.TestGenerationLimitManager
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

    fun fuzzing(parameters: List<Type>, isCancelled: () -> Boolean, limitManager: TestGenerationLimitManager): Flow<FuzzingExecutionFeedback> = flow {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        ServerSocket(0).use { serverSocket ->
            logger.debug { "Server port: ${serverSocket.localPort}" }
            val manager = try {
                PythonWorkerManager(
                    serverSocket,
                    pythonPath,
                    limitManager.until,
                    { constructEvaluationInput(it) },
                    timeoutForRun.toInt()
                )
            } catch (_: TimeoutException) {
                logger.info { "Cannot connect to python executor" }
                return@flow
            }
            logger.info { "Executor manager was created successfully" }

            fun runWithFuzzedValues(
                arguments: List<PythonFuzzedValue>,
            ): PythonEvaluationResult? {
                val argumentValues = arguments.map { PythonTreeModel(it.tree, it.tree.type) }
                logger.debug(argumentValues.map { it.tree }.toString())
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
                return try {
                    manager.run(functionArguments, localAdditionalModules)
                } catch (_: TimeoutException) {
                    logger.info { "Fuzzing process was interrupted by timeout" }
                    null
                }
            }

            fun handleExecutionResult(
                result: PythonEvaluationResult,
                arguments: List<PythonFuzzedValue>,
                description: PythonMethodDescription,
            ): Pair<PythonExecutionResult, Boolean> {
                val executionFeedback: FuzzingExecutionFeedback
                val fuzzingFeedback: PythonFeedback

                when(result) {
                    is PythonEvaluationError -> {
                        val utError = UtError(
                            "Error evaluation: ${result.status}, ${result.message}",
                            Throwable(result.stackTrace.joinToString("\n"))
                        )
                        logger.debug(result.stackTrace.joinToString("\n"))

                        limitManager.addSuccessExecution()
                        executionFeedback = InvalidExecution(utError)
                        fuzzingFeedback = PythonFeedback(control = Control.PASS)
                        return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), true)
                    }

                    is PythonEvaluationTimeout -> {
                        val utError = UtError(result.message, Throwable())
                        limitManager.addInvalidExecution()
                        executionFeedback = InvalidExecution(utError)
                        fuzzingFeedback = PythonFeedback(control = Control.PASS)
                        return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), false)
                    }

                    is PythonEvaluationSuccess -> {
                        val coveredInstructions = result.coverage.coveredInstructions
                        executionFeedback = handleSuccessResult(
                            arguments,
                            parameters,
                            result,
                            description,
                        )

                        val trieNode: Trie.Node<Instruction> = description.tracer.add(coveredInstructions)
                        when (executionFeedback) {
                            is ValidExecution -> {
                                limitManager.addSuccessExecution()
                                if (trieNode.count > 1) {
                                    fuzzingFeedback = PythonFeedback(control = Control.CONTINUE, result = trieNode)
                                    return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), false)
                                }
                            }

                            is ArgumentsTypeErrorFeedback -> {
                                fuzzingFeedback = PythonFeedback(control = Control.PASS)
                                return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), false)
                            }

                            is TypeErrorFeedback -> {
                                limitManager.addInvalidExecution()
                                fuzzingFeedback = PythonFeedback(control = Control.PASS)
                                return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), false)
                            }

                            is InvalidExecution -> {
                                limitManager.addInvalidExecution()
                                fuzzingFeedback = PythonFeedback(control = Control.CONTINUE)
                                return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), false)
                            }
                        }
                        fuzzingFeedback = PythonFeedback(control = Control.CONTINUE, result = trieNode)
                        return Pair(PythonExecutionResult(executionFeedback, fuzzingFeedback), true)
                    }
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
                val result = runWithFuzzedValues(emptyList())
                result?.let {
                    val (executionResult, needToEmit) = handleExecutionResult(result, emptyList(), pmd)
                    if (needToEmit) {
                        emit(executionResult.fuzzingExecutionFeedback)
                    }
                }
                manager.disconnect()
            } else {
                try {
                    PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
                        if (isCancelled()) {
                            logger.info { "Fuzzing process was interrupted" }
                            manager.disconnect()
                            return@PythonFuzzing PythonFeedback(control = Control.STOP)
                        }

                        val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                        val mem = cache.get(pair)
                        if (mem != null) {
                            logger.debug("Repeat in fuzzing")
                            return@PythonFuzzing mem.fuzzingPlatformFeedback
                        }

                        val result = runWithFuzzedValues(arguments)
                        if (result == null) {  // timeout
                            manager.disconnect()
                            return@PythonFuzzing PythonFeedback(control = Control.STOP)
                        }

                        val (executionResult, needToEmit) = handleExecutionResult(result, arguments, description)
                        cache.add(pair, executionResult)
                        if (needToEmit) {
                            emit(executionResult.fuzzingExecutionFeedback)
                        }
                        return@PythonFuzzing executionResult.fuzzingPlatformFeedback
                    }.fuzz(pmd)
                } catch (ex: Exception) {  // NoSeedValueException
                    logger.info { "Cannot fuzz values for types: $parameters" }
                }
                manager.disconnect()
            }
        }
    }
}