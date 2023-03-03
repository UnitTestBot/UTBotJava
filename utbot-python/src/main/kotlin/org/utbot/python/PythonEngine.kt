package org.utbot.python

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.evaluation.PythonCodeExecutor
import org.utbot.python.evaluation.PythonCodeSocketExecutor
import org.utbot.python.evaluation.PythonEvaluationError
import org.utbot.python.evaluation.PythonEvaluationSuccess
import org.utbot.python.evaluation.PythonEvaluationTimeout
import org.utbot.python.evaluation.PythonWorker
import org.utbot.python.evaluation.PythonWorkerManager
import org.utbot.python.evaluation.serialiation.MemoryDump
import org.utbot.python.evaluation.serialiation.toPythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.fuzzing.*
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket

private val logger = KotlinLogging.logger {}
private const val MAX_CACHE_SIZE = 200

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<PythonFuzzedConcreteValue>,
    private val timeoutForRun: Long,
    private val initialCoveredLines: Set<Int>,
    private val pythonTypeStorage: PythonTypeStorage,
) {

    private val cache = mutableMapOf<Pair<PythonMethodDescription, List<PythonTreeWrapper>>, PythonExecutionResult>()

    private fun addExecutionToCache(key: Pair<PythonMethodDescription, List<PythonTreeWrapper>>, result: PythonExecutionResult) {
        cache[key] = result
        if (cache.size > MAX_CACHE_SIZE) {
            val elemToDelete = cache.keys.maxBy { (_, args) ->
                args.fold(0) { acc, arg -> arg.commonDiversity(acc) }
            }
            cache.remove(elemToDelete)
        }
    }

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
        types: List<Type>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: PythonMethodDescription,
        hasThisObject: Boolean,
        summary: List<String>,
    ): FuzzingExecutionFeedback {
        val prohibitedExceptions = listOf(
            "builtins.AttributeError",
            "builtins.TypeError"
        )

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

    fun constructEvaluationInput(pythonWorker: PythonWorker): PythonCodeExecutor {
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
        val coveredLines = initialCoveredLines.toMutableSet()

        ServerSocket(0).use { serverSocket ->
            logger.info { "Server port: ${serverSocket.localPort}" }
            val manager = PythonWorkerManager(
                serverSocket,
                pythonPath,
                until
            ) { constructEvaluationInput(it) }
            logger.info { "Executor manager was created successfully" }

            fun fuzzingResultHandler(
                description: PythonMethodDescription,
                arguments: List<PythonFuzzedValue>
            ): PythonExecutionResult {
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
                val evaluationResult = manager.run(functionArguments, localAdditionalModules)
                return when (evaluationResult) {
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
                        coveredInstructions.forEach { coveredLines.add(it.lineNumber) }

                        val summary = arguments
                            .zip(methodUnderTest.arguments)
                            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

                        val hasThisObject = methodUnderTest.hasThisArgument

                        when (val result = handleSuccessResult(
                            parameters,
                            evaluationResult,
                            description,
                            hasThisObject,
                            summary
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
            }

            val pmd = PythonMethodDescription(
                methodUnderTest.name,
                parameters,
                fuzzedConcreteValues,
                pythonTypeStorage,
                Trie(Instruction::id)
            )

            if (parameters.isEmpty()) {
                fuzzingResultHandler(pmd, emptyList())
                manager.disconnect()
            } else {
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

                    val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                    val mem = cache[pair]
                    if (mem != null) {
                        logger.debug("Repeat in fuzzing")
                        emit(mem.fuzzingExecutionFeedback)
                        return@PythonFuzzing mem.fuzzingPlatformFeedback
                    }
                    val result = fuzzingResultHandler(description, arguments)
                    addExecutionToCache(pair, result)
                    emit(result.fuzzingExecutionFeedback)
                    return@PythonFuzzing result.fuzzingPlatformFeedback

                }.fuzz(pmd)
            }
        }
    }
}