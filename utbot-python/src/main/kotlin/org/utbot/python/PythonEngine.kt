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
import java.lang.Long.max

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
    private val pythonTypeStorage: PythonTypeStorage? = null,
) {

    private data class JobResult(
        val evalResult: PythonEvaluationResult,
        val values: List<FuzzedValue>,
        val thisObject: UtModel?,
        val modelList: List<UtModel>
    )

    private fun suggestExecutionName(
        description: PythonMethodDescription,
        jobResult: JobResult,
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

    private fun handleSuccessResult(
        types: List<Type>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: PythonMethodDescription,
        jobResult: JobResult,
        summary: List<String>,
    ): FuzzingExecutionFeedback {
        val (resultJSON, isException, coverage) = evaluationResult

        val prohibitedExceptions = listOf(
            "builtins.AttributeError",
            "builtins.TypeError"
        )

        if (isException && (resultJSON.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.pythonTypeRepresentation() }
            }. Exception type: ${resultJSON.type.name}"

            logger.info(errorMessage)
            return TypeErrorFeedback(errorMessage)
        }

        val executionResult =
            if (isException) {
                UtExplicitlyThrownException(Throwable(resultJSON.output.type.toString()), false)
            }
            else {
                val outputType = resultJSON.type
                val resultAsModel = PythonTreeModel(
                    resultJSON.output,
                    outputType
                )
                UtExecutionSuccess(resultAsModel)
            }

        val testMethodName = suggestExecutionName(methodUnderTestDescription, jobResult, executionResult)

        val utFuzzedExecution = UtFuzzedExecution(
            stateBefore = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            stateAfter = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            result = executionResult,
            coverage = coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) }
        )
        return ValidExecution(utFuzzedExecution)
    }

    fun fuzzing(parameters: List<Type>, isCancelled: () -> Boolean, until: Long): Flow<FuzzingExecutionFeedback> = flow {
        val additionalModules = parameters.flatMap { it.pythonModules() }

        val pmd = PythonMethodDescription(
            methodUnderTest.name,
            parameters,
            fuzzedConcreteValues,
            pythonTypeStorage!!,
        )

        val coveredLines = initialCoveredLines.toMutableSet()
        var sourceLinesCount = Long.MAX_VALUE

        PythonFuzzing(pmd.pythonTypeStorage) { description, arguments ->
            if (isCancelled() || System.currentTimeMillis() >= until) {
                logger.info { "Fuzzing process was interrupted" }
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }

            val argumentValues = arguments.map {
                PythonTreeModel(it.tree, it.tree.type)
            }
            val summary =
                arguments.zip(methodUnderTest.arguments)
                    .mapNotNull { it.first.summary?.replace("%var%", it.second.name) }

            val (thisObject, modelList) =
                if (methodUnderTest.containingPythonClassId == null)
                    Pair(null, argumentValues)
                else
                    Pair(argumentValues[0], argumentValues.drop(1))

            val argumentModules = argumentValues.flatMap {
                it.allContainingClassIds
            }.map {
                it.moduleName
            }
            val localAdditionalModules = (additionalModules + argumentModules).toSet()

            val evaluationInput = EvaluationInput(
                methodUnderTest,
                argumentValues,
                directoriesForSysPath,
                moduleToImport,
                pythonPath,
                timeoutForRun,
                thisObject,
                modelList,
                argumentValues.map { FuzzedValue(it) },
                localAdditionalModules
            )

            val process = startEvaluationProcess(evaluationInput)
            val startedTime = System.currentTimeMillis()
            val wait = max(TIMEOUT, timeoutForRun - (System.currentTimeMillis() - startedTime))
            val jobResult = JobResult(
                getEvaluationResult(evaluationInput, process, wait),
                evaluationInput.values,
                evaluationInput.thisObject,
                evaluationInput.modelList
            )

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

                    val result = handleSuccessResult(parameters, evaluationResult, description, jobResult, summary)
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
                            return@PythonFuzzing PythonFeedback(control = Control.STOP)
                        }
                        is InvalidExecution -> {
                            return@PythonFuzzing PythonFeedback(control = Control.PASS)
                        }
                    }
                }
            }
        }.fuzz(pmd)
    }
}