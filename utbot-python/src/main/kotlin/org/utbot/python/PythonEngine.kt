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
import org.utbot.python.evaluation.serialiation.MemoryDump
import org.utbot.python.evaluation.serialiation.toPythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.fuzzing.PythonFeedback
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzing
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo

private val logger = KotlinLogging.logger {}

sealed interface FuzzingExecutionFeedback
class ValidExecution(val utFuzzedExecution: UtFuzzedExecution): FuzzingExecutionFeedback
class InvalidExecution(val utError: UtError): FuzzingExecutionFeedback
class TypeErrorFeedback(val message: String) : FuzzingExecutionFeedback
class ArgumentsTypeErrorFeedback(val message: String) : FuzzingExecutionFeedback

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

    private fun constructEvaluationInput(): PythonCodeExecutor {
        return PythonCodeSocketExecutor(
            methodUnderTest,
            moduleToImport,
            pythonPath,
            directoriesForSysPath,
            timeoutForRun,
        )
    }

    fun fuzzing(parameters: List<Type>, isCancelled: () -> Boolean, until: Long): Flow<FuzzingExecutionFeedback> = flow {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        val pmd = PythonMethodDescription(
            methodUnderTest.name,
            parameters,
            fuzzedConcreteValues,
            pythonTypeStorage,
            Trie(Instruction::id)
        )

        val coveredLines = initialCoveredLines.toMutableSet()

        val codeExecutor = constructEvaluationInput()
        PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
            if (isCancelled()) {
                logger.info { "Fuzzing process was interrupted" }
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }
            if (System.currentTimeMillis() >= until) {
                logger.info { "Fuzzing process was interrupted by timeout" }
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }

            val argumentValues = arguments.map { PythonTreeModel(it.tree, it.tree.type) }
            val argumentModules = argumentValues
                .flatMap { it.allContainingClassIds }
                .map { it.moduleName }
            val localAdditionalModules = (additionalModules + argumentModules).toSet()

            val (thisObject, modelList) =
                if (methodUnderTest.hasThisArgument)
                    Pair(argumentValues[0], argumentValues.drop(1))
                else
                    Pair(null, argumentValues)
            val functionArguments = FunctionArguments(thisObject, methodUnderTest.thisObjectName, modelList, methodUnderTest.argumentsNames)

            when (val evaluationResult = codeExecutor.run(functionArguments, localAdditionalModules)) {
                is PythonEvaluationError -> {
                    val utError = UtError(
                        "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}",
                        Throwable(evaluationResult.stackTrace.joinToString("\n"))
                    )
                    logger.debug(evaluationResult.stackTrace.joinToString("\n"))
                    emit(InvalidExecution(utError))
                    return@PythonFuzzing PythonFeedback(control = Control.PASS)
                }

                is PythonEvaluationTimeout -> {
                    val utError = UtError(evaluationResult.message, Throwable())
                    emit(InvalidExecution(utError))
                    return@PythonFuzzing PythonFeedback(control = Control.PASS)
                }

                is PythonEvaluationSuccess -> {
                    val coveredInstructions = evaluationResult.coverage.coveredInstructions
                    coveredInstructions.forEach { coveredLines.add(it.lineNumber) }

                    val summary = arguments
                        .zip(methodUnderTest.arguments)
                        .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

                    val hasThisObject = codeExecutor.method.hasThisArgument

                    when (val result = handleSuccessResult(parameters, evaluationResult, description, hasThisObject, summary)) {
                        is ValidExecution -> {
                            logger.debug { arguments }
                            val trieNode: Trie.Node<Instruction> = description.tracer.add(coveredInstructions)
                            emit(result)
                            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE, result = trieNode)
                        }
                        is ArgumentsTypeErrorFeedback, is TypeErrorFeedback -> {
                            emit(result)
                            return@PythonFuzzing PythonFeedback(control = Control.PASS)
                        }
                        is InvalidExecution -> {
                            emit(result)
                            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                        }
                    }
                }
            }
        }.fuzz(pmd)
        if (codeExecutor is PythonCodeSocketExecutor) {
            codeExecutor.stop()
        }
    }
}