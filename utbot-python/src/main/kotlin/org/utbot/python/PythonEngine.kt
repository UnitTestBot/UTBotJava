package org.utbot.python

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.TimeoutException
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
import org.utbot.python.utils.startProcess
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

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
        val coveredLines = initialCoveredLines.toMutableSet()
        val logfile = TemporaryFileManager.createTemporaryFile("","utbot_executor", "log", true)

        ServerSocket(0).use { serverSocket ->
            logger.info { "Server port: ${serverSocket.localPort}" }
            val processStartTime = System.currentTimeMillis()
            val process = startProcess(listOf(
                pythonPath,
                "-m", "utbot_executor",
                "localhost",
                serverSocket.localPort.toString(),
                "--logfile", logfile.absolutePath,
                "--loglevel", "DEBUG",
                ))
            val timeout = until - processStartTime
            val workerSocket = try {
                serverSocket.soTimeout = timeout.toInt()
                serverSocket.accept()
            } catch (e: SocketTimeoutException) {
                val processHasExited = process.waitFor(timeout, TimeUnit.MILLISECONDS)
                if (!processHasExited) {
                    process.destroy()
                }
                throw TimeoutException("Worker not connected")
            }
            logger.info { "Worker connected successfully" }
            val pythonWorker = PythonWorker(workerSocket)
            val codeExecutor = constructEvaluationInput(pythonWorker)
            logger.info { "Executor was created successfully" }

            fun fuzzingResultHandler(
                description: PythonMethodDescription,
                arguments: List<PythonFuzzedValue>
            ): PythonFeedback {
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

                return when (val evaluationResult = codeExecutor.run(functionArguments, localAdditionalModules)) {
                    is PythonEvaluationError -> {
                        val utError = UtError(
                            "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}",
                            Throwable(evaluationResult.stackTrace.joinToString("\n"))
                        )
                        logger.debug(evaluationResult.stackTrace.joinToString("\n"))
                        PythonFeedback(control = Control.PASS, executionFeedback = InvalidExecution(utError))
                    }

                    is PythonEvaluationTimeout -> {
                        val utError = UtError(evaluationResult.message, Throwable())
                        PythonFeedback(control = Control.PASS, executionFeedback = InvalidExecution(utError))
                    }

                    is PythonEvaluationSuccess -> {
                        val coveredInstructions = evaluationResult.coverage.coveredInstructions
                        coveredInstructions.forEach { coveredLines.add(it.lineNumber) }

                        val summary = arguments
                            .zip(methodUnderTest.arguments)
                            .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

                        val hasThisObject = codeExecutor.method.hasThisArgument

                        when (val result = handleSuccessResult(
                            parameters,
                            evaluationResult,
                            description,
                            hasThisObject,
                            summary
                        )) {
                            is ValidExecution -> {
                                val trieNode: Trie.Node<Instruction> = description.tracer.add(coveredInstructions)
                                PythonFeedback(control = Control.CONTINUE, result = trieNode, result)
                            }
                            is ArgumentsTypeErrorFeedback, is TypeErrorFeedback -> {
                                PythonFeedback(control = Control.PASS, executionFeedback = result)
                            }
                            is InvalidExecution -> {
                                PythonFeedback(control = Control.CONTINUE, executionFeedback = result)
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

            val checkedModels = mutableMapOf<Pair<PythonMethodDescription, List<PythonTreeWrapper>>, PythonFeedback>()
            if (parameters.isEmpty()) {
                fuzzingResultHandler(pmd, emptyList())
            } else {
                PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
                    if (isCancelled()) {
                        logger.info { "Fuzzing process was interrupted" }
                        return@PythonFuzzing PythonFeedback(control = Control.STOP, executionFeedback = null)
                    }
                    if (System.currentTimeMillis() >= until) {
                        logger.info { "Fuzzing process was interrupted by timeout" }
                        return@PythonFuzzing PythonFeedback(control = Control.STOP, executionFeedback = null)
                    }

                    val pair = Pair(description, arguments.map { PythonTreeWrapper(it.tree) })
                    val mem = checkedModels[pair]
                    if (mem != null) {
                        logger.debug("Repeat in fuzzing")
                        if (mem.executionFeedback != null)
                            emit(mem.executionFeedback)

                        return@PythonFuzzing mem
                    }
                    val result = fuzzingResultHandler(description, arguments)
                    checkedModels[pair] = result

                    if (result.executionFeedback != null)
                        emit(result.executionFeedback)

                    return@PythonFuzzing result
                }.fuzz(pmd)
                if (codeExecutor is PythonCodeSocketExecutor) {
                    codeExecutor.stop()
                }
            }
        }
    }
}