package org.utbot.python.typing

import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.PythonMethod
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.utils.*
import java.io.File

object AnnotationFinder {

    fun findAnnotations(
        argInfoCollector: ArgInfoCollector,
        methodUnderTest: PythonMethod,
        existingAnnotations: Map<String, NormalizedPythonAnnotation>,
        moduleToImport: String,
        directoriesForSysPath: List<String>,
        pythonPath: String,
        fileOfMethod: String,
        isCancelled: () -> Boolean,
        storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine>
    ): Sequence<Map<String, NormalizedPythonAnnotation>> {

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

    private const val INF = 1000

    private fun increaseValue(map: MutableMap<NormalizedPythonAnnotation, Int>, key: NormalizedPythonAnnotation) {
        if (map[key] == INF || key == pythonAnyClassId)
            return
        map[key] = (map[key] ?: 0) + 1
    }

    private fun getInitCandidateMap(): MutableMap<NormalizedPythonAnnotation, Int> {
        val candidates = mutableMapOf<NormalizedPythonAnnotation, Int>() // key: type, value: priority
        PythonTypesStorage.builtinTypes.associateByTo(
            destination = candidates,
            { AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(PythonClassId("builtins.$it")) },
            { 0 }
        )
        return candidates
    }

    private fun candidatesMapToRating(candidates: Map<NormalizedPythonAnnotation, Int>) =
        candidates.toList().sortedByDescending { it.second }.map { it.first }

    private fun increaseForProjectClasses(candidates: MutableMap<NormalizedPythonAnnotation, Int>) {
        candidates.keys.forEach { typeName ->
            if (PythonTypesStorage.isClassFromProject(typeName))
                increaseValue(candidates, typeName)
        }
    }

    private fun increaseForGenerics(candidates: MutableMap<NormalizedPythonAnnotation, Int>) {
        candidates.keys.forEach { typeName ->
            if (isGeneric(typeName))
                increaseValue(candidates, typeName)
        }
    }

    private fun getFirstLevelCandidates(
        hints: List<ArgInfoCollector.Hint>?
    ): List<NormalizedPythonAnnotation> {
        val candidates = getInitCandidateMap()
        hints?.forEach { hint ->
            when (hint) {
                is ArgInfoCollector.Type ->
                    candidates[AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(hint.type)] = INF
                is ArgInfoCollector.Method -> {
                    val typesWithMethod = PythonTypesStorage.findTypeWithMethod(hint.name)
                    typesWithMethod.forEach { increaseValue(candidates, it) }

                    if (hint.name == "__iter__")
                        increaseForGenerics(candidates)
                }
                is ArgInfoCollector.Field -> {
                    val typesWithField = PythonTypesStorage.findTypeWithField(hint.name)
                    typesWithField.forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.FunctionArg -> {
                    PythonTypesStorage.findTypeByFunctionWithArgumentPosition(
                        hint.name,
                        argumentPosition = hint.index
                    ).forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.FunctionRet -> {
                    PythonTypesStorage.findTypeByFunctionReturnValue(
                        hint.name
                    ).forEach { increaseValue(candidates, it) }
                }
            }
        }
        increaseForProjectClasses(candidates)
        return candidatesMapToRating(candidates)
    }

    private fun getGeneralTypeRating(
        argInfoCollector: ArgInfoCollector
    ): List<NormalizedPythonAnnotation> {
        val candidates = getInitCandidateMap()
        argInfoCollector.getAllGeneralHints().map { hint ->
            when (hint) {
                is ArgInfoCollector.Type ->
                    increaseValue(candidates, AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(hint.type))
                is ArgInfoCollector.Function -> {
                    listOf(
                        PythonTypesStorage.findTypeByFunctionReturnValue(hint.name),
                        PythonTypesStorage.findTypeByFunctionWithArgumentPosition(hint.name)
                    ).flatten().forEach { increaseValue(candidates, it) }
                }
                is ArgInfoCollector.Method -> {
                    PythonTypesStorage.findTypeWithMethod(hint.name).forEach {
                        increaseValue(candidates, it)
                    }
                }
                is ArgInfoCollector.Field -> {
                    PythonTypesStorage.findTypeWithField(hint.name).forEach {
                        increaseValue(candidates, it)
                    }
                }
            }
        }
        increaseForProjectClasses(candidates)
        return candidatesMapToRating(candidates)
    }

    private const val MAX_CANDIDATES_FOR_PARAM = 100

    private fun getArgCandidates(
        generalTypeRating: List<NormalizedPythonAnnotation>,
        argStorages: List<ArgInfoCollector.Hint> = emptyList(),
        userAnnotation: NormalizedPythonAnnotation? = null
    ): List<NormalizedPythonAnnotation> {
        val root =
            if (userAnnotation != null)
                sequenceOf(userAnnotation).iterator()
            else
                getFirstLevelCandidates(argStorages).asSequence().iterator()

        val bfsQueue = ArrayDeque(listOf(root))
        val result = mutableListOf<NormalizedPythonAnnotation>()
        while (result.size < MAX_CANDIDATES_FOR_PARAM && bfsQueue.isNotEmpty()) {
            val curIter = bfsQueue.removeFirst()
            if (!curIter.hasNext())
                continue

            val value = curIter.next()
            result.add(value)
            bfsQueue.addLast(curIter)

            val asGeneric = parseGeneric(value) ?: continue
            if (!asGeneric.args.any { it == pythonAnyClassId })
                continue

            val argCandidates = asGeneric.args.map {
                if (it == pythonAnyClassId)
                    generalTypeRating
                else
                    listOf(it)
            }
            val toAnnotation =
                when (asGeneric) {
                    is ListAnnotation -> ListAnnotation::pack
                    is DictAnnotation -> DictAnnotation::pack
                    is SetAnnotation -> SetAnnotation::pack
                }
            val nextGenericCandidates = PriorityCartesianProduct(argCandidates).getSequence().map {
                NormalizedPythonAnnotation(toAnnotation(it).toString())
            }
            bfsQueue.add(nextGenericCandidates.iterator())
        }
        return result
    }

    private fun findTypeCandidates(
        argInfoCollector: ArgInfoCollector,
        existingAnnotations: Map<String, NormalizedPythonAnnotation>
    ): Map<String, List<NormalizedPythonAnnotation>> {
        val storageMap = argInfoCollector.getAllArgHints()
        val generalTypeRating = getGeneralTypeRating(argInfoCollector)
        val userAnnotations = existingAnnotations.entries.associate {
            it.key to getArgCandidates(generalTypeRating, userAnnotation = it.value)
        }
        val annotationCombinations = storageMap.entries.associate { (name, storages) ->
            name to getArgCandidates(generalTypeRating, storages)
        }
        return userAnnotations + annotationCombinations
    }
}