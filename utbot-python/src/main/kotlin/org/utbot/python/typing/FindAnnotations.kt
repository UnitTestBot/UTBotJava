package org.utbot.python.typing

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.PythonMethod
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.utils.AnnotationNormalizer
import java.io.File

object AnnotationFinder {

    private const val INF = 1000

    private fun increaseValue(map: MutableMap<String, Int>, key: String) {
        if (map[key] == INF || key == pythonAnyClassId.name)
            return
        map[key] = (map[key] ?: 0) + 1
    }

    private fun getInitCandidateMap(): MutableMap<String, Int> {
        val candidates = mutableMapOf<String, Int>() // key: type, value: priority
        PythonTypesStorage.builtinTypes.associateByTo(
            destination = candidates,
            { AnnotationNormalizer.substituteTypes("builtins.$it") },
            { 0 }
        )
        return candidates
    }

    private fun candidatesMapToRating(candidates: Map<String, Int>): List<String> =
        candidates.toList().sortedByDescending { it.second }.map { it.first }

    private fun firstLevelCandidates(
        storages: List<ArgInfoCollector.BaseStorage>?
    ): List<String> {
        val candidates = getInitCandidateMap()
        storages?.forEach { argInfoStorage ->
            when (argInfoStorage) {
                is ArgInfoCollector.Type -> candidates[argInfoStorage.name] = INF
                is ArgInfoCollector.Method -> {
                    val typesWithMethod = PythonTypesStorage.findTypeWithMethod(argInfoStorage.name)
                    typesWithMethod.forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.Field -> {
                    val typesWithField = PythonTypesStorage.findTypeWithField(argInfoStorage.name)
                    typesWithField.forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.FunctionArg -> {
                    PythonTypesStorage.findTypeByFunctionWithArgumentPosition(
                        argInfoStorage.name,
                        argumentPosition = argInfoStorage.index
                    ).forEach { increaseValue(candidates, it.name) }
                }
                is ArgInfoCollector.FunctionRet -> {
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
        return candidatesMapToRating(candidates)
    }

    private fun getGeneralTypeRating(
        argInfoCollector: ArgInfoCollector
    ): List<String> {
        val candidates = getInitCandidateMap()
        argInfoCollector.getAllGeneralStorages().map { generalStorage ->
            when (generalStorage) {
                is ArgInfoCollector.Type -> increaseValue(candidates, generalStorage.name)
                is ArgInfoCollector.Function -> {
                    listOf(
                        PythonTypesStorage.findTypeByFunctionReturnValue(generalStorage.name),
                        PythonTypesStorage.findTypeByFunctionWithArgumentPosition(generalStorage.name)
                    ).flatten().forEach { increaseValue(candidates, it.name) }
                }
            }
        }
        return candidatesMapToRating(candidates)
    }

    private fun findTypeCandidates(
        argInfoCollector: ArgInfoCollector,
        existingAnnotations: Map<String, String>
    ): Map<String, List<String>> {
        val storageMap = argInfoCollector.getAllArgStorages()
        val userAnnotations = existingAnnotations.entries.associate {
            it.key to listOf(it.value)
        }
        val annotationCombinations = storageMap.entries.associate { (name, storages) ->
            name to firstLevelCandidates(storages)
        }
        return userAnnotations + annotationCombinations
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
    ): Sequence<Map<String, PythonClassId>> {

        val annotationsToCheck = findTypeCandidates(argInfoCollector, existingAnnotations)

        return MypyAnnotations.getCheckedByMypyAnnotations(
            methodUnderTest,
            annotationsToCheck,
            moduleToImport,
            directoriesForSysPath + listOf(File(fileOfMethod).parentFile.path),
            pythonPath,
            isCancelled,
            storageForMypyMessages
        )
    }
}