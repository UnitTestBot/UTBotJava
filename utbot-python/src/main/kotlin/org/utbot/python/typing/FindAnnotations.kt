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
        directoriesForSysPath: Set<String>,
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

    private fun increaseValue(
        map: MutableMap<NormalizedPythonAnnotation, Double>,
        key: NormalizedPythonAnnotation,
        by: Double
    ) {
        if (key == pythonAnyClassId)
            return
        map[key] = (map[key] ?: 0.0) + by
    }

    private fun getInitCandidateMap(): MutableMap<NormalizedPythonAnnotation, Double> {
        val candidates = mutableMapOf<NormalizedPythonAnnotation, Double>()  // key: type, value: priority
        PythonTypesStorage.builtinTypes.associateByTo(
            destination = candidates,
            { AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(PythonClassId("builtins.$it")) },
            { 0.0 }
        )
        return candidates
    }

    private fun candidatesMapToRating(candidates: Map<NormalizedPythonAnnotation, Double>) =
        candidates.toList().sortedByDescending { it.second }.map { it.first }

    private fun increaseForProjectClasses(candidates: MutableMap<NormalizedPythonAnnotation, Double>, by: Double) {
        candidates.keys.forEach { typeName ->
            if (PythonTypesStorage.isClassFromProject(typeName))
                increaseValue(candidates, typeName, by)
        }
    }

    private fun increaseForGenerics(candidates: MutableMap<NormalizedPythonAnnotation, Double>, by: Double) {
        candidates.keys.forEach { typeName ->
            if (isGeneric(typeName))
                increaseValue(candidates, typeName, by)
        }
    }

    private fun calcAdd(foundCandidates: Int): Double =
        if (foundCandidates == 0) 0.0 else 1.0 / foundCandidates

    const val EPS = 1e-6

    private fun getFirstLevelCandidates(
        hints: List<ArgInfoCollector.Hint>?
    ): List<NormalizedPythonAnnotation> {
        val candidates = getInitCandidateMap()
        hints?.forEach { hint ->
            var isIter = false
            val foundCandidates : Set<NormalizedPythonAnnotation> =
                when (hint) {
                    is ArgInfoCollector.Type ->
                        setOf(AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(hint.type))
                    is ArgInfoCollector.Method -> {
                        if (hint.name == "__iter__")
                            isIter = true
                        PythonTypesStorage.findTypeWithMethod(hint.name)
                    }
                    is ArgInfoCollector.Field -> PythonTypesStorage.findTypeWithField(hint.name)
                    is ArgInfoCollector.FunctionArg -> {
                        PythonTypesStorage.findTypeByFunctionWithArgumentPosition(
                            hint.name,
                            argumentPosition = hint.index
                        )
                    }
                    is ArgInfoCollector.FunctionRet -> PythonTypesStorage.findTypeByFunctionReturnValue(hint.name)
                    else -> emptySet()
                }
            val add = calcAdd(foundCandidates.size)
            foundCandidates.forEach { increaseValue(candidates, it, add) }
            if (isIter)
                increaseForGenerics(candidates, EPS)
        }
        increaseForProjectClasses(candidates, EPS)
        return candidatesMapToRating(candidates)
    }

    private fun getGeneralTypeRating(
        argInfoCollector: ArgInfoCollector
    ): List<NormalizedPythonAnnotation> {
        val candidates = getInitCandidateMap()
        argInfoCollector.getAllGeneralHints().map { hint ->
            val foundCandidates: Set<NormalizedPythonAnnotation> =
                when (hint) {
                    is ArgInfoCollector.Type ->
                        setOf(AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(hint.type))
                    is ArgInfoCollector.Function ->
                        listOf(
                            PythonTypesStorage.findTypeByFunctionReturnValue(hint.name),
                            PythonTypesStorage.findTypeByFunctionWithArgumentPosition(hint.name)
                        ).flatten().toSet()
                    is ArgInfoCollector.Method -> PythonTypesStorage.findTypeWithMethod(hint.name)
                    is ArgInfoCollector.Field -> PythonTypesStorage.findTypeWithField(hint.name)
                    else -> emptySet()
                }
            val add = calcAdd(foundCandidates.size)
            foundCandidates.forEach { increaseValue(candidates, it, add) }
        }
        increaseForProjectClasses(candidates, EPS)
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

            val asGeneric = parseGeneric(value)
            if (asGeneric == null || !asGeneric.args.any { it == pythonAnyClassId })
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
            bfsQueue.addFirst(nextGenericCandidates.iterator())
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