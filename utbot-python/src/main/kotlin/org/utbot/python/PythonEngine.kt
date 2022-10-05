package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.python.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.python.PythonTreeModel
import org.utbot.framework.plugin.api.python.pythonAnyClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.fuzz
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.providers.defaultPythonModelProvider
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.MethodBasedNameSuggester
import org.utbot.summary.fuzzer.names.ModelBasedNameSuggester
import org.utbot.fuzzer.FuzzedValue
import java.lang.Long.max

private val logger = KotlinLogging.logger {}
const val CHUNK_SIZE = 15

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
        val evalResult: EvaluationResult,
        val values: List<FuzzedValue>,
        val thisObject: UtModel?,
        val modelList: List<UtModel>
    )

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
        }.iterator()

        val coveredLines = initialCoveredLines.toMutableSet()
        while (evaluationInputIterator.hasNext()) {
            val chunk = mutableListOf<EvaluationInput>()
            while (evaluationInputIterator.hasNext() && chunk.size < CHUNK_SIZE)
                chunk += evaluationInputIterator.next()

            val coveredBefore = coveredLines.size
            val processes = chunk.map { evaluationInput ->
                startEvaluationProcess(evaluationInput)
            }
            val startedTime = System.currentTimeMillis()
            val results = (processes zip chunk).map { (process, evaluationInput) ->
                val wait = max(10, timeoutForRun - (System.currentTimeMillis() - startedTime))
                val evalResult = getEvaluationResult(evaluationInput, process, wait)
                JobResult(
                    evalResult,
                    evaluationInput.values,
                    evaluationInput.thisObject,
                    evaluationInput.modelList
                )
            }
            results.forEach { jobResult ->
                if (jobResult.evalResult is EvaluationError) {
                    yield(UtError(jobResult.evalResult.reason, Throwable()))
                } else {
                    val (resultJSON, isException, coverage) = jobResult.evalResult as EvaluationSuccess

                    coverage.coveredInstructions.forEach { coveredLines.add(it.lineNumber) }

                    val prohibitedExceptions = listOf(
                        "builtins.AttributeError",
                        "builtins.TypeError"
                    )
                    if (isException && (resultJSON.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
                        logger.debug("Evaluation with prohibited exception. Substituted types: ${
                            types.joinToString { it.name }
                        }. Exception type: ${resultJSON.type.name}")
                        return@sequence
                    }

                    val result =
                        if (isException)
                            UtExplicitlyThrownException(Throwable(resultJSON.output.type.toString()), false) // TODO:
                        else {
                            val outputType = resultJSON.type
                            val resultAsModel = PythonTreeModel(
                                resultJSON.output,
                                outputType
                            )
                            UtExecutionSuccess(resultAsModel)
                        }

                    val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester())
                    val testMethodName = try {
                        nameSuggester.flatMap { it.suggest(methodUnderTestDescription, jobResult.values, result) }.firstOrNull()
                    } catch (t: Throwable) {
                        null
                    }

                    yield(
                        UtExecution(
                            stateBefore = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
                            stateAfter = EnvironmentModels(jobResult.thisObject, jobResult.modelList, emptyMap()),
                            result = result,
                            coverage = coverage,
                            testMethodName = testMethodName?.testName?.camelToSnakeCase(),
                            displayName = testMethodName?.displayName,
                        )
                    )
                }
            }
            val coveredAfter = coveredLines.size

            if (coveredAfter == coveredBefore)
                return@sequence
        }
    }
}
