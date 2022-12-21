package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.*
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.providers.defaultPythonModelProvider
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.MethodBasedNameSuggester
import org.utbot.summary.fuzzer.names.ModelBasedNameSuggester
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.lang.Long.max

private val logger = KotlinLogging.logger {}
const val CHUNK_SIZE = 15
const val TIMEOUT: Long = 10

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<FuzzedConcreteValue>,
    private val selectedTypeMap: Map<String, NormalizedPythonAnnotation>,
    private val timeoutForRun: Long,
    private val initialCoveredLines: Set<Int>
) {

    private data class JobResult(
        val evalResult: PythonEvaluationResult,
        val values: List<FuzzedValue>,
        val thisObject: UtModel?,
        val modelList: List<UtModel>
    )

    private fun suggestExecutionName(
        methodUnderTestDescription: FuzzedMethodDescription,
        jobResult: JobResult,
        executionResult: UtExecutionResult
    ): TestSuggestedInfo? {
        val nameSuggester = sequenceOf(
            ModelBasedNameSuggester(),
            MethodBasedNameSuggester()
        )

        val testMethodName = try {
            nameSuggester
                .flatMap { it.suggest(methodUnderTestDescription, jobResult.values, executionResult) }
                .firstOrNull()
        } catch (t: Throwable) {
            null
        }
        return testMethodName
    }

    private fun createEvaluationInputIterator(
        methodUnderTestDescription: FuzzedMethodDescription
    ): Sequence<EvaluationInput> {
        val additionalModules = selectedTypeMap.values.flatMap {
            getModulesFromAnnotation(it)
        }.toSet()

        val evaluationInputIterator = fuzz(methodUnderTestDescription, defaultPythonModelProvider).map { values ->
            val parameterValues = values.map { it.model }
            val (thisObject, modelList) =
                if (methodUnderTest.containingPythonClassId == null)
                    Pair(null, parameterValues)
                else
                    Pair(parameterValues[0], parameterValues.drop(1))

            EvaluationInput(
                methodUnderTest,
                parameterValues,
                directoriesForSysPath,
                moduleToImport,
                pythonPath,
                timeoutForRun,
                thisObject,
                modelList,
                values,
                additionalModules
            )
        }

        return evaluationInputIterator
    }

    private fun handleSuccessResult(
        types: List<NormalizedPythonAnnotation>,
        evaluationResult: PythonEvaluationSuccess,
        methodUnderTestDescription: FuzzedMethodDescription,
        jobResult: JobResult
    ): UtResult {
        val (resultJSON, isException, coverage) = evaluationResult

        val prohibitedExceptions = listOf(
            "builtins.AttributeError",
            "builtins.TypeError"
        )

        if (isException && (resultJSON.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.name }
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

        val testMethodName = suggestExecutionName(methodUnderTestDescription, jobResult, executionResult)

        return UtFuzzedExecution(
            stateBefore = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            stateAfter = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
            result = executionResult,
            coverage = coverage,
            testMethodName = testMethodName?.testName?.camelToSnakeCase(),
            displayName = testMethodName?.displayName,
        )
    }

    fun fuzzing(): Sequence<UtResult> = sequence {
        val types = methodUnderTest.arguments.map {
            selectedTypeMap[it.name] ?: pythonAnyClassId
        }

        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            pythonAnyClassId,
            types,
            fuzzedConcreteValues
        ).apply {
            compilableName = methodUnderTest.name // what's the difference with ordinary name?
            parameterNameMap = { index -> methodUnderTest.arguments.getOrNull(index)?.name }
        }

        val evaluationInputIterator = createEvaluationInputIterator(methodUnderTestDescription)

        val coveredLines = initialCoveredLines.toMutableSet()
        evaluationInputIterator.chunked(CHUNK_SIZE).forEach { chunk ->
            val coveredBefore = coveredLines.size

            // TODO: maybe reuse processes for next chunk?
            val processes = chunk.map { evaluationInput ->
                startEvaluationProcess(evaluationInput)
            }
            val startedTime = System.currentTimeMillis()

            val results = (processes zip chunk).map { (process, evaluationInput) ->
                val wait = max(TIMEOUT, timeoutForRun - (System.currentTimeMillis() - startedTime))
                val evalResult = getEvaluationResult(evaluationInput, process, wait)
                JobResult(
                    evalResult,
                    evaluationInput.values,
                    evaluationInput.thisObject,
                    evaluationInput.modelList
                )
            }

            results.forEach { jobResult ->
                when(val evaluationResult = jobResult.evalResult) {
                    is PythonEvaluationError -> {
                        if (evaluationResult.status != 0) {
                            val errorMessage = "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}"
                            logger.info { "Python evaluation error: $errorMessage" }
                            yield(UtError(
                                errorMessage,
                                Throwable(evaluationResult.stackTrace.joinToString("\n"))
                            ))
                        } else {
                            logger.info { "Python evaluation error: ${evaluationResult.message}" }
                            yield(
                                UtError(
                                    evaluationResult.message,
                                    Throwable(evaluationResult.stackTrace.joinToString("\n"))
                                )
                            )
                        }
                    }
                    is PythonEvaluationSuccess -> {
                        evaluationResult.coverage.coveredInstructions.forEach { coveredLines.add(it.lineNumber) }
                        yield(
                            handleSuccessResult(types, evaluationResult, methodUnderTestDescription, jobResult)
                        )
                    }
                    is PythonEvaluationTimeout -> {
                        yield(UtError(evaluationResult.message, Throwable()))
                    }
                }
            }

            val coveredAfter = coveredLines.size

            if (coveredAfter == coveredBefore)
                return@sequence
        }
    }
}
