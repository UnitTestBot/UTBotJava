package org.utbot.python

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.names.MethodBasedNameSuggester
import org.utbot.fuzzer.names.ModelBasedNameSuggester
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.providers.concreteTypesModelProvider
import org.utbot.python.providers.substituteTypesByIndex
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.typing.PythonTypesStorage
import org.utbot.python.typing.ReturnRenderType
import org.utbot.python.typing.StubFileFinder
import java.io.File

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val testSourceRoot: String,
    private val directoriesForSysPath: List<String>,
    private val moduleToImport: String,
    private val pythonPath: String,
    private val projectRoot: String,
    private val fileOfMethod: String
) {
    fun fuzzing(): Sequence<PythonResult> = sequence {
        val argumentTypes = methodUnderTest.arguments.map {
            annotationToClassId(
                it.annotation,
                pythonPath,
                projectRoot,
                fileOfMethod,
                directoriesForSysPath,
                testSourceRoot
            )
        }
        val existingAnnotations = mutableMapOf<String, String>()
        argumentTypes.forEachIndexed { index, classId ->
            if (classId != pythonAnyClassId)
                existingAnnotations[methodUnderTest.arguments[index].name] = classId.name
        }

        val argInfoCollector = ArgInfoCollector(methodUnderTest, argumentTypes)
        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            pythonAnyClassId,
            argumentTypes,
            argInfoCollector.getConstants()
        ).apply {
            compilableName = methodUnderTest.name // what's the difference with ordinary name?
            parameterNameMap = { index -> methodUnderTest.arguments.getOrNull(index)?.name }
        }

        val annotations: Sequence<Map<String, ClassId>> =
            if (existingAnnotations.size == methodUnderTest.arguments.size)
                sequenceOf(
                    existingAnnotations.mapValues { entry -> ClassId(entry.value) }
                )
            else
                joinAnnotations(
                    argInfoCollector,
                    methodUnderTest,
                    existingAnnotations,
                    testSourceRoot,
                    moduleToImport,
                    directoriesForSysPath,
                    pythonPath,
                    fileOfMethod
                )

        // model provider argwith fallback?
        // attempts?

        var testsGenerated = 0

        annotations.forEach { types ->
            val classIds = methodUnderTest.arguments.map {
                types[it.name] ?: pythonAnyClassId
            }
            val substitutedDescription = substituteTypesByIndex(methodUnderTestDescription, classIds)
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

                    // some types cannot be used as return types in tests (like socket or memoryview)
                    val outputType = ClassId(resultJSON.type)
                    if (PythonTypesStorage.getTypeByName(outputType)?.returnRenderType == ReturnRenderType.NONE)
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

    private val inf = 1000

    private fun increaseValue(map: MutableMap<String, Int>, key: String) {
        if (map[key] == inf)
            return
        map[key] = (map[key] ?: 0) + 1
    }

    private fun findTypeCandidates(
        storages: List<ArgInfoCollector.BaseStorage>?
    ): List<String> {
        val candidates = mutableMapOf<String, Int>() // key: type, value: priority
        PythonTypesStorage.builtinTypes.associateByTo(destination = candidates, { it }, { 0 })
        storages?.forEach { argInfoStorage ->
            when (argInfoStorage) {
                is ArgInfoCollector.TypeStorage -> candidates[argInfoStorage.name] = inf
                is ArgInfoCollector.MethodStorage -> {
                    val typesWithMethod = PythonTypesStorage.findTypeWithMethod(argInfoStorage.name)
                    typesWithMethod.forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.FieldStorage -> {
                    val typesWithField = PythonTypesStorage.findTypeWithField(argInfoStorage.name)
                    typesWithField.forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.FunctionArgStorage -> {
                    StubFileFinder.findTypeByFunctionWithArgumentPosition(
                        argInfoStorage.name,
                        argumentPosition = argInfoStorage.index
                    ).forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.FunctionRetStorage -> {
                    StubFileFinder.findTypeByFunctionReturnValue(
                        argInfoStorage.name
                    ).forEach { increaseValue(candidates, it) }
                }
            }
        }
        return candidates.toList().sortedByDescending { it.second }.map { it.first }
    }

    private fun joinAnnotations(
        argInfoCollector: ArgInfoCollector,
        methodUnderTest: PythonMethod,
        existingAnnotations: Map<String, String>,
        testSourceRoot: String,
        moduleToImport: String,
        directoriesForSysPath: List<String>,
        pythonPath: String,
        fileOfMethod: String
    ): Sequence<Map<String, ClassId>> {
        val storageMap = argInfoCollector.getAllStorages()
        val userAnnotations = existingAnnotations.entries.associate {
            it.key to listOf(it.value)
        }
        val annotationCombinations = storageMap.entries.associate { (name, storages) ->
            name to findTypeCandidates(storages)
        }

        return MypyAnnotations.mypyCheckAnnotations(
            methodUnderTest,
            userAnnotations + annotationCombinations,
            testSourceRoot,
            moduleToImport,
            directoriesForSysPath + listOf(File(fileOfMethod).parentFile.path),
            pythonPath
        )
    }
}
