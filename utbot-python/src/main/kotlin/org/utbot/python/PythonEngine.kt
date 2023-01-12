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
import org.utbot.framework.plugin.api.UtResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.fuzz
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.fuzzing.PythonFeedback
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzing
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.providers.PythonFuzzedMethodDescription
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.lang.Long.max

private val logger = KotlinLogging.logger {}
const val TIMEOUT: Long = 10

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<PythonFuzzedConcreteValue>,
    private val selectedTypeMap: Map<String, NormalizedPythonAnnotation>,
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
        description: FuzzedMethodDescription,
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

    private fun handleSuccessResultNew(
        types: List<Type>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: PythonMethodDescription,
        jobResult: JobResult,
        summary: List<String>,
    ): UtResult {
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

            return UtError(errorMessage, Throwable())
        }

        val executionResult =
            if (isException)
                UtExplicitlyThrownException(Throwable(resultJSON.output.type.toString()), false)
            else {
                val outputType = resultJSON.type
                val resultAsModel = PythonTreeModel(
                    resultJSON.output,
                    outputType
                )
                UtExecutionSuccess(resultAsModel)
            }

        val _methodUnderTestDescription = PythonFuzzedMethodDescription(
            methodUnderTestDescription.name,
            pythonAnyClassId,
            emptyList(),
            emptyList()
        )

        val testMethodName = suggestExecutionName(_methodUnderTestDescription, jobResult, executionResult)

        return UtFuzzedExecution(
            stateBefore = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            stateAfter = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            result = executionResult,
            coverage = coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) }
        )
    }

    fun newFuzzing(parameters: List<Type>, isCancelled: () -> Boolean, until: Long): Flow<UtResult> = flow {
        var additionalModules = selectedTypeMap.values.flatMap {
            getModulesFromAnnotation(it)
        }.toSet()

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
            additionalModules = (additionalModules + argumentModules).toSet()

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
                additionalModules
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
                    if (evaluationResult.status != 0) {
                        emit(
                            UtError(
                                "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}",
                                Throwable(evaluationResult.stackTrace.joinToString("\n"))
                            )
                        )
                    } else {
                        emit(
                            UtError(
                                evaluationResult.message,
                                Throwable(evaluationResult.stackTrace.joinToString("\n"))
                            )
                        )
                    }
                }

                is PythonEvaluationSuccess -> {
                    evaluationResult.coverage.coveredInstructions.forEach { coveredLines.add(it.lineNumber) }
                    val instructionsCount = evaluationResult.coverage.instructionsCount
                    if (instructionsCount != null) {
                        sourceLinesCount = instructionsCount
                    }

                    val result = handleSuccessResultNew(parameters, evaluationResult, description, jobResult, summary)
                    emit(result)
                    if (result is UtError) {
                        return@PythonFuzzing PythonFeedback(control = Control.PASS)
                    }
                }

                is PythonEvaluationTimeout -> {
                    emit(
                        UtError(evaluationResult.message, Throwable())
                    )
                }
            }

            if (coveredLines.size.toLong() == sourceLinesCount) {
                return@PythonFuzzing PythonFeedback(control = Control.STOP)
            }

            return@PythonFuzzing PythonFeedback(control = Control.CONTINUE)

        }.fuzz(pmd)
    }
}