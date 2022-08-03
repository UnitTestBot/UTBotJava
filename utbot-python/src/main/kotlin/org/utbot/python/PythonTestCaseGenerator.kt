package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.typing.PythonTypesStorage
import org.utbot.python.typing.StubFileFinder
import java.io.File

object PythonTestCaseGenerator {
    lateinit var testSourceRoot: String
    lateinit var directoriesForSysPath: List<String>
    lateinit var moduleToImport: String
    lateinit var pythonPath: String
    lateinit var projectRoot: String
    lateinit var fileOfMethod: String
    lateinit var isCancelled: () -> Boolean

    fun init(
        testSourceRoot: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String,
        projectRoot: String,
        fileOfMethod: String,
        isCancelled: () -> Boolean
    ) {
        this.testSourceRoot = testSourceRoot
        this.directoriesForSysPath = directoriesForSysPath
        this.moduleToImport = moduleToImport
        this.pythonPath = pythonPath
        this.projectRoot = projectRoot
        this.fileOfMethod = fileOfMethod
        this.isCancelled = isCancelled
    }

    fun generate(method: PythonMethod): PythonTestSet {
        val initialArgumentTypes = method.arguments.map {
            annotationToClassId(
                it.annotation,
                pythonPath,
                projectRoot,
                fileOfMethod,
                directoriesForSysPath,
                testSourceRoot
            )
        }
        val argInfoCollector = ArgInfoCollector(method, initialArgumentTypes)
        val annotationSequence = getAnnotations(method, initialArgumentTypes, argInfoCollector, isCancelled)

        val executions = mutableListOf<PythonExecution>()
        val errors = mutableListOf<PythonError>()

        annotationSequence.forEach typeSelection@{ annotations ->
            if (isCancelled())
                return@typeSelection

            val engine = PythonEngine(
                method,
                testSourceRoot,
                directoriesForSysPath,
                moduleToImport,
                pythonPath,
                argInfoCollector.getConstants(),
                annotations
            )

            engine.fuzzing().forEach fuzzing@{
                if (isCancelled())
                    return@fuzzing
                when (it) {
                    is PythonExecution -> executions += it
                    is PythonError -> errors += it
                }
            }
        }

        return PythonTestSet(method, executions, errors)
    }

    fun getAnnotations(
        method: PythonMethod,
        initialArgumentTypes: List<ClassId>,
        argInfoCollector: ArgInfoCollector,
        isCancelled: () -> Boolean
    ): Sequence<Map<String, ClassId>> {
        val existingAnnotations = mutableMapOf<String, String>()
        initialArgumentTypes.forEachIndexed { index, classId ->
            if (classId != pythonAnyClassId)
                existingAnnotations[method.arguments[index].name] = classId.name
        }

        return if (existingAnnotations.size == method.arguments.size)
            sequenceOf(
                existingAnnotations.mapValues { entry -> ClassId(entry.value) }
            )
        else
            findAnnotations(
                argInfoCollector,
                method,
                existingAnnotations,
                testSourceRoot,
                moduleToImport,
                directoriesForSysPath,
                pythonPath,
                fileOfMethod,
                isCancelled
            )
    }


    private const val inf = 1000

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

    private fun findAnnotations(
        argInfoCollector: ArgInfoCollector,
        methodUnderTest: PythonMethod,
        existingAnnotations: Map<String, String>,
        testSourceRoot: String,
        moduleToImport: String,
        directoriesForSysPath: List<String>,
        pythonPath: String,
        fileOfMethod: String,
        isCancelled: () -> Boolean
    ): Sequence<Map<String, ClassId>> {
        val storageMap = argInfoCollector.getAllStorages()
        val userAnnotations = existingAnnotations.entries.associate {
            it.key to listOf(it.value)
        }
        val annotationCombinations = storageMap.entries.associate { (name, storages) ->
            name to findTypeCandidates(storages)
        }

        return MypyAnnotations.getCheckedByMypyAnnotations(
            methodUnderTest,
            userAnnotations + annotationCombinations,
            testSourceRoot,
            moduleToImport,
            directoriesForSysPath + listOf(File(fileOfMethod).parentFile.path),
            pythonPath,
            isCancelled
        )
    }
}