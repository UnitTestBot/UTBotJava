package org.utbot.python

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.fuzz
import org.utbot.python.code.MemoryDump
import org.utbot.python.code.toPythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.fuzzing.PythonFeedback
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonFuzzing
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo

private val logger = KotlinLogging.logger {}
const val TIMEOUT: Long = 10

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

        if (evaluationResult.isException && (evaluationResult.result.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.pythonTypeRepresentation() }
            }. Exception type: ${evaluationResult.result.type.name}"

            logger.info(errorMessage)
            return TypeErrorFeedback(errorMessage)
        }

        val executionResult =
            if (evaluationResult.isException) {
                UtExplicitlyThrownException(Throwable(evaluationResult.result.output.type.toString()), false)
            }
            else {
                UtExecutionSuccess(PythonTreeModel(evaluationResult.result.output))
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

    private fun constructEvaluationInput(arguments: List<PythonFuzzedValue>, additionalModules: List<String>): EvaluationInput {
        val argumentValues = arguments.map { PythonTreeModel(it.tree, it.tree.type) }

        val (thisObject, modelList) =
            if (methodUnderTest.hasThisArgument)
                Pair(argumentValues[0], argumentValues.drop(1))
            else
                Pair(null, argumentValues)

        val argumentModules = argumentValues
            .flatMap { it.allContainingClassIds }
            .map { it.moduleName }
        val localAdditionalModules = (additionalModules + argumentModules).toSet()

        return EvaluationInput(
            methodUnderTest,
            directoriesForSysPath,
            moduleToImport,
            pythonPath,
            timeoutForRun,
            thisObject,
            modelList,
            argumentValues.map { FuzzedValue(it) },
            localAdditionalModules
        )
    }

    fun fuzzing(parameters: List<Type>, isCancelled: () -> Boolean, until: Long): Flow<FuzzingExecutionFeedback> = flow {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        val pmd = PythonMethodDescription(
            methodUnderTest.name,
            parameters,
            fuzzedConcreteValues,
            pythonTypeStorage,
        )

        val coveredLines = initialCoveredLines.toMutableSet()
        var sourceLinesCount = Long.MAX_VALUE

        PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
            if (isCancelled()) {
                logger.info { "Fuzzing process was interrupted" }
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }
            if (System.currentTimeMillis() >= until) {
                logger.info { "Fuzzing process was interrupted by timeout" }
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }

            val evaluationInput = constructEvaluationInput(arguments, additionalModules)
            val jobResult = evaluationInput.evaluate()

            when (val evaluationResult = jobResult.evalResult) {
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
                    evaluationResult.coverage.coveredInstructions.forEach { coveredLines.add(it.lineNumber) }
                    val instructionsCount = evaluationResult.coverage.instructionsCount
                    if (instructionsCount != null) {
                        sourceLinesCount = instructionsCount
                    }

                    val summary = arguments
                        .zip(methodUnderTest.arguments)
                        .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

                    val hasThisObject = jobResult.thisObject != null
                    val result = handleSuccessResult(parameters, evaluationResult, description, hasThisObject, summary)
                    emit(result)

                    if (coveredLines.size.toLong() == sourceLinesCount) {
                        return@PythonFuzzing PythonFeedback(control = Control.STOP)
                    }

                    when (result) {
                        is ValidExecution -> {
                            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                        }
                        is ArgumentsTypeErrorFeedback -> {
                            return@PythonFuzzing PythonFeedback(control = Control.PASS)
                        }
                        is TypeErrorFeedback -> {
                            return@PythonFuzzing PythonFeedback(control = Control.PASS)
                        }
                        is InvalidExecution -> {
                            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)
                        }
                    }
                }
            }
        }.fuzz(pmd)
    }
}