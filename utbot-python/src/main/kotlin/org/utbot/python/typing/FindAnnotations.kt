package org.utbot.python.typing

import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.PythonMethod
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.utils.AnnotationNormalizer
import java.io.File

object AnnotationFinder {

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
        PythonTypesStorage.builtinTypes.associateByTo(
            destination = candidates,
            { AnnotationNormalizer.substituteTypes("builtins.$it") },
            { 0 }
        )
        storages?.forEach { argInfoStorage ->
            when (argInfoStorage) {
                is ArgInfoCollector.TypeStorage -> candidates[argInfoStorage.name] = inf
                is ArgInfoCollector.MethodStorage -> {
                    val typesWithMethod = PythonTypesStorage.findTypeWithMethod(argInfoStorage.name)
                    typesWithMethod.forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.FieldStorage -> {
                    val typesWithField = PythonTypesStorage.findTypeWithField(argInfoStorage.name)
                    typesWithField.forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.FunctionArgStorage -> {
                    PythonTypesStorage.findTypeByFunctionWithArgumentPosition(
                        argInfoStorage.name,
                        argumentPosition = argInfoStorage.index
                    ).forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.FunctionRetStorage -> {
                    PythonTypesStorage.findTypeByFunctionReturnValue(
                        argInfoStorage.name
                    ).forEach { increaseValue(candidates, it.name) }
                }
            }
        }
        candidates.keys.forEach { typeName ->
            if (PythonTypesStorage.isFromProject(typeName))
                increaseValue(candidates, typeName)
        }
        return candidates.toList().sortedByDescending { it.second }.map { it.first }
    }

    fun findAnnotations(
        argInfoCollector: ArgInfoCollector,
        methodUnderTest: PythonMethod,
        existingAnnotations: Map<String, String>,
        moduleToImport: String,
        directoriesForSysPath: List<String>,
        pythonPath: String,
        fileOfMethod: String,
        isCancelled: () -> Boolean,
        storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine>
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
            moduleToImport,
            directoriesForSysPath + listOf(File(fileOfMethod).parentFile.path),
            pythonPath,
            isCancelled,
            storageForMypyMessages
        )
    }
}