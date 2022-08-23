package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
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
private const val CHUNK_SIZE = 10

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<FuzzedConcreteValue>,
    private val selectedTypeMap: Map<String, NormalizedPythonAnnotation>,
    private val timeoutForRun: Long
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

        val evaluationInputInterator = fuzz(methodUnderTestDescription, defaultPythonModelProvider).map { values ->
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

        val coveredLines = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
        while (evaluationInputInterator.hasNext()) {
            val chunk = mutableListOf<EvaluationInput>()
            while (evaluationInputInterator.hasNext() && chunk.size < CHUNK_SIZE)
                chunk += evaluationInputInterator.next()

            val coveredBefore = coveredLines.size
            val processes = chunk.map { evaluationInput ->
                startEvaluationProcess(evaluationInput)
            }
            val startedTime = System.currentTimeMillis()
            val results = (processes zip chunk).map { (process, evaluationInput) ->
                val wait = max(10, timeoutForRun - (System.currentTimeMillis() - startedTime))
                JobResult(
                    getEvaluationResult(evaluationInput, process, wait),
                    evaluationInput.values,
                    evaluationInput.thisObject,
                    evaluationInput.modelList
                )
            }
            val coveredAfter = coveredLines.size
            results.forEach { jobResult ->
                if (jobResult.evalResult is EvaluationError) {
                    yield(UtError("EvaluationError", Throwable())) // TODO: make better error description
                } else {
                    val (resultJSON, isException, coverage) = jobResult.evalResult as EvaluationSuccess

                    val prohibitedExceptions = listOf(
                        "builtins.AttributeError",
                        "builtins.TypeError"
                    )
                    if (isException && (resultJSON.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
                        logger.debug("Evaluation with prohibited exception. Substituted types: ${
                            types.joinToString { it.name }
                        }")
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

            if (coveredAfter == coveredBefore)
                return@sequence
        }
    }
}
