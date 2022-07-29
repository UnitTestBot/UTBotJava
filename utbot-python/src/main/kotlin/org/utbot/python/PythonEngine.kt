package org.utbot.python

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.CartesianProduct
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.names.MethodBasedNameSuggester
import org.utbot.fuzzer.names.ModelBasedNameSuggester
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.code.StubFileFinder
import org.utbot.python.providers.concreteTypesModelProvider
import org.utbot.python.providers.substituteTypesByIndex
import kotlin.random.Random

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val testSourceRoot: String,
    private val directoriesForSysPath: List<String>,
    private val moduleToImport: String,
    private val pythonPath: String
) {
    fun fuzzing(): Sequence<PythonResult> = sequence {
        val returnType = methodUnderTest.returnType ?: ClassId("")
        val argumentTypes = methodUnderTest.arguments.map { it.type }

        val argInfoCollector = ArgInfoCollector(methodUnderTest)
        val argMethodAnnotations = argInfoCollector.getMethods().entries.associate {
            it.key to (it.value.map { storage ->
                StubFileFinder.findTypeWithMethod(storage.methodName)
            }.reduceRightOrNull { acc, set -> acc.intersect(set) }?.toList() ?: emptyList())
        }
//        val argsAttributeAnnotations = argInfoCollector.getFields().entries.associate {
//            it.key to (it.value.map { storage ->
//                StubFileFinder.findTypeWithField(storage.name)
//            }.reduceOrNull { acc, set -> acc.intersect(set) }?.toList() ?: emptyList())
//        }
//        val argsFunctionAnnotations = argInfoCollector.getFunctionArgs().entries.associate {
//            it.key to (it.value.map { storage ->
//                StubFileFinder.findTypeByFunctionWithArgumentPosition(storage.name, argumentPosition = storage.index)
//            }.reduceOrNull { acc, set -> acc.intersect(set) }?.toList() ?: emptyList())
//        }

        val methodData = MypyAnnotations.mypyCheckAnnotations(
            methodUnderTest,
            argMethodAnnotations,
            testSourceRoot,
            moduleToImport,
            directoriesForSysPath,
            pythonPath
        )
        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            returnType,
            argumentTypes,
            argInfoCollector.getConstants()
        ).apply {
            compilableName = methodUnderTest.name // what's the difference with ordinary name?
            parameterNameMap = { index -> methodUnderTest.arguments.getOrNull(index)?.name }
        }

        // model provider argwith fallback?
        // attempts?

        var testsGenerated = 0

        val suggestedTypes = argInfoCollector.suggestBasedOnConstants().map { it.toList() }

        if (suggestedTypes.any { it.isEmpty() })
            return@sequence

        CartesianProduct(suggestedTypes, Random(0L)).forEach { types ->
            val substitutedDescription = substituteTypesByIndex(methodUnderTestDescription, types)
            fuzz(substitutedDescription, concreteTypesModelProvider).forEach { values ->
                val modelList = values.map { it.model }

                // execute method to get function return
                // what if exception happens?
                val evalResult = PythonEvaluation.evaluate(
                    methodUnderTest,
                    modelList,
                    testSourceRoot,
                    directoriesForSysPath,
                    moduleToImport,
                    pythonPath
                )
                if (evalResult is EvaluationError)
                    return@sequence

                val (resultJSON, isException) = evalResult as EvaluationSuccess

                if (isException) {
                    yield(PythonError(UtError(resultJSON.output, Throwable()), modelList))
                } else {

                    if (PythonTypesStorage.typeNameMap[resultJSON.type]?.useAsReturn != true)
                        return@sequence

                    val resultAsModel = PythonDefaultModel(resultJSON.output, "")
                    val result = UtExecutionSuccess(resultAsModel)

                    val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester())
                    val testMethodName = try {
                        nameSuggester.flatMap { it.suggest(methodUnderTestDescription, values, result) }.firstOrNull()
                    } catch (t: Throwable) {
                        null
                    }

                    yield(
                        PythonExecution(
                            UtExecution(
                                stateBefore = EnvironmentModels(null, modelList, emptyMap()),
                                stateAfter = EnvironmentModels(null, modelList, emptyMap()),
                                result = result,
                                instrumentation = emptyList(),
                                path = mutableListOf(), // ??
                                fullPath = emptyList(), // ??
                                testMethodName = testMethodName?.testName,
                                displayName = testMethodName?.displayName
                            ),
                            modelList
                        )
                    )
                }

                testsGenerated += 1
                if (testsGenerated == 100)
                    return@sequence
            }
        }
    }
}
