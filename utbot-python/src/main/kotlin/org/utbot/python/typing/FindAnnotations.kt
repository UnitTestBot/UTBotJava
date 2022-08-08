package org.utbot.python.typing

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.PythonMethod
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.utils.*
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

    private fun increaseForProjectClasses(candidates: MutableMap<String, Int>) {
        candidates.keys.forEach { typeName ->
            if (PythonTypesStorage.isFromProject(typeName))
                increaseValue(candidates, typeName)
        }
    }

    private fun getFirstLevelCandidates(
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
        increaseForProjectClasses(candidates)
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
                is ArgInfoCollector.Method -> {
                    PythonTypesStorage.findTypeWithMethod(generalStorage.name).forEach {
                        increaseValue(candidates, it.name)
                    }
                }
                is ArgInfoCollector.Field -> {
                    PythonTypesStorage.findTypeWithField(generalStorage.name).forEach {
                        increaseValue(candidates, it.name)
                    }
                }
            }
        }
        increaseForProjectClasses(candidates)
        return candidatesMapToRating(candidates)
    }

    private const val MAX_CANDIDATES_FOR_PARAM = 100

    private fun getArgCandidates(
        generalTypeRating: List<String>,
        argStorages: List<ArgInfoCollector.BaseStorage> = emptyList(),
        userAnnotation: String? = null
    ): List<String> {
        val root =
            if (userAnnotation != null)
                sequenceOf(userAnnotation).iterator()
            else
                getFirstLevelCandidates(argStorages).asSequence().iterator()

        val bfsQueue = ArrayDeque(listOf(root))
        val result = mutableListOf<String>()
        while (result.size < MAX_CANDIDATES_FOR_PARAM && bfsQueue.isNotEmpty()) {
            val curIter = bfsQueue.removeFirst()
            if (!curIter.hasNext())
                continue

            val value = curIter.next()
            result.add(value)
            bfsQueue.addLast(curIter)

            val asGeneric = parseGeneric(value) ?: continue
            if (!asGeneric.args.any { it == pythonAnyClassId.name })
                continue

            val argCandidates = asGeneric.args.map {
                if (it == pythonAnyClassId.name)
                    generalTypeRating
                else
                    listOf(it)
            }
            val toAnnotation =
                when (asGeneric) {
                    is ListAnnotation -> ListAnnotation::unparse
                    is DictAnnotation -> DictAnnotation::unparse
                    is SetAnnotation -> SetAnnotation::unparse
                }
            val nextGenericCandidates = PriorityCartesianProduct(argCandidates).getSequence().map {
                toAnnotation(it).toString()
            }
            bfsQueue.add(nextGenericCandidates.iterator())
        }
        return result
    }

    private fun findTypeCandidates(
        argInfoCollector: ArgInfoCollector,
        existingAnnotations: Map<String, String>
    ): Map<String, List<String>> {
        val storageMap = argInfoCollector.getAllArgStorages()
        val generalTypeRating = getGeneralTypeRating(argInfoCollector)
        val userAnnotations = existingAnnotations.entries.associate {
            it.key to getArgCandidates(generalTypeRating, userAnnotation = it.value)
        }
        val annotationCombinations = storageMap.entries.associate { (name, storages) ->
            name to getArgCandidates(generalTypeRating, storages)
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
    ): Sequence<Map<String, ClassId>> {

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