package org.utbot.python

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.names.MethodBasedNameSuggester
import org.utbot.fuzzer.names.ModelBasedNameSuggester
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.providers.defaultPythonModelProvider

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val fuzzedConcreteValues: List<FuzzedConcreteValue>,
    private val selectedTypeMap: Map<String, NormalizedPythonAnnotation>
) {
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

        fuzz(methodUnderTestDescription, defaultPythonModelProvider).forEach { values ->
            val parameterValues = values.map { it.model }

            val (thisObject, modelList) =
                if (methodUnderTest.containingPythonClassId == null)
                    Pair(null, parameterValues)
                else
                    Pair(parameterValues[0], parameterValues.drop(1))

            val evalResult = PythonEvaluation.evaluate(
                methodUnderTest,
                parameterValues,
                directoriesForSysPath,
                moduleToImport,
                pythonPath,
                additionalModules
            )
            if (evalResult is EvaluationError) {
                yield(UtError("EvaluationError", Throwable())) // TODO: make better error description
            } else {
                val (resultJSON, isException) = evalResult as EvaluationSuccess

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
                    nameSuggester.flatMap { it.suggest(methodUnderTestDescription, values, result) }.firstOrNull()
                } catch (t: Throwable) {
                    null
                }

                yield(
                    UtExecution(
                        stateBefore = EnvironmentModels(thisObject, modelList, emptyMap()),
                        stateAfter = EnvironmentModels(thisObject, modelList, emptyMap()),
                        result = result,
                        instrumentation = emptyList(),
                        path = mutableListOf(), // ??
                        fullPath = emptyList(), // ??
                        testMethodName = testMethodName?.testName,
                        displayName = testMethodName?.displayName,
                    )
                )
            }
        }
    }
}
