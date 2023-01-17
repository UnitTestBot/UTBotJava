package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorResult
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.getBoundedParameters
import org.utbot.python.newtyping.general.getOrigin
import org.utbot.python.newtyping.inference.collectBoundsFromEdges

private val logger = KotlinLogging.logger {}

class TypeRating(scores: List<Pair<Type, Double>>) {
    val typesInOrder: List<Pair<Double, Type>> = scores.map { Pair(it.second, it.first) }.sortedBy { -it.first }
    val types: List<Type> = typesInOrder.map { it.second }
}

data class TypeRatingPosition(
    val typeRating: TypeRating,
    val pos: Int
) {
    val hasNext: Boolean
        get() = pos < typeRating.types.size - 1
    val curPenalty: Double
        get() = typeRating.typesInOrder.first().first - typeRating.typesInOrder[pos].first
    val type: Type
        get() = typeRating.types[pos]

    fun makeMove(): TypeRatingPosition {
        assert(hasNext)
        return TypeRatingPosition(typeRating, pos + 1)
    }
}

// TODO: memory usage can be reduced
class CandidateGraph(
    anyNodes: List<AnyTypeNode>,
    initialRating: List<Type>,
    storage: PythonTypeStorage,
) {
    private val typeRatings: List<TypeRating> =
        anyNodes.map { createTypeRating(initialRating, it.lowerBounds, it.upperBounds, storage, it.nestedLevel) }
    private val vertices: MutableList<CandidateGraphVertex> =
        mutableListOf(CandidateGraphVertex(typeRatings.map { TypeRatingPosition(it, 0) }))
    private var lastUsed: Int = -1
    private var lastExpanded: Int = -1

    fun getNext(): List<Type>? {
        if (lastUsed < lastExpanded) {
            lastUsed += 1
            return vertices[lastUsed].types
        }
        if (lastExpanded == vertices.size - 1)
            return null

        lastExpanded += 1
        vertices[lastExpanded].generateSuccessors().forEach(::insertVertexIfNotAlready)

        return getNext()
    }

    private fun insertVertexIfNotAlready(vertex: CandidateGraphVertex) {
        for (i in 0 until vertices.size) {
            if (vertices[i] == vertex)
                return
            if (vertices[i].penalty > vertex.penalty) {
                vertices.add(i, vertex)
                return
            }
        }
        vertices.add(vertex)
    }

    init {
        logger.debug("Created new CandidateGraph")
    }
}

data class CandidateGraphVertex(val positions: List<TypeRatingPosition>) {
    val penalty by lazy {
        positions.fold(0.0) { acc, typeRatingPosition -> acc + typeRatingPosition.curPenalty }
    }

    fun generateSuccessors(): List<CandidateGraphVertex> =
        positions.mapNotNull { pos ->
            CandidateGraphVertex(
                positions.map {
                    if (it == pos) {
                        if (!pos.hasNext)
                            return@mapNotNull null
                        pos.makeMove()
                    } else
                        it
                }
            )
        }

    val types by lazy {
        positions.map { it.type }
    }
}

private const val MAX_NESTING = 3

private fun changeScores(
    initialRating: List<Type>,
    storage: PythonTypeStorage,
    bounds: List<Type>,
    hintScores: MutableMap<PythonTypeWrapperForEqualityCheck, Double>,
    withPenalty: Boolean,
    isUpper: Boolean
) {
    bounds.forEach { constraint ->
        val (fitting, notFitting) = initialRating.partition { typeFromList ->
            val type = DefaultSubstitutionProvider.substitute(
                typeFromList,
                typeFromList.getBoundedParameters().associateWith { pythonAnyType }
            )
            if (isUpper)
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(constraint, type, storage)
            else
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, constraint, storage)
        }
        if (withPenalty)
            notFitting.forEach {
                val wrapper = PythonTypeWrapperForEqualityCheck(it)
                hintScores[wrapper] = (hintScores[wrapper] ?: 0.0) - 1
            }
        fitting.forEach {
            val wrapper = PythonTypeWrapperForEqualityCheck(it)
            hintScores[wrapper] = (hintScores[wrapper] ?: 0.0) + 1.0 / fitting.size
        }
    }
}

fun createTypeRating(
    initialRating: List<Type>,
    lowerBounds: List<Type>,
    upperBounds: List<Type>,
    storage: PythonTypeStorage,
    level: Int,
    withPenalty: Boolean = true
): TypeRating {
    val hintScores = mutableMapOf<PythonTypeWrapperForEqualityCheck, Double>()
    changeScores(initialRating, storage, lowerBounds, hintScores, withPenalty, isUpper = false)
    changeScores(initialRating, storage, upperBounds, hintScores, withPenalty, isUpper = true)
    val scores: List<Pair<Type, Double>> = initialRating.mapNotNull { typeFromList ->
        if (level == MAX_NESTING && typeFromList.getBoundedParameters().isNotEmpty())
            return@mapNotNull null
        val type = DefaultSubstitutionProvider.substitute(
            typeFromList,
            typeFromList.getBoundedParameters().associateWith { pythonAnyType }
        )
        val wrapper = PythonTypeWrapperForEqualityCheck(type)
        Pair(type, hintScores[wrapper] ?: 0.0)
    }
    return TypeRating(scores)
}

fun createGeneralTypeRating(hintCollectorResult: HintCollectorResult, storage: PythonTypeStorage): List<Type> {
    val allLowerBounds: MutableList<Type> = mutableListOf()
    val allUpperBounds: MutableList<Type> = mutableListOf()
    hintCollectorResult.allNodes.forEach { node ->
        val (lowerFromEdges, upperFromEdges) = collectBoundsFromEdges(node)
        allLowerBounds.addAll((lowerFromEdges + node.lowerBounds + node.partialType).filter {
            !typesAreEqual(it, pythonAnyType)
        })
        allUpperBounds.addAll((upperFromEdges + node.upperBounds + node.partialType).filter {
            !typesAreEqual(it, pythonAnyType)
        })
    }
    val int = storage.pythonInt
    val listOfAny = DefaultSubstitutionProvider.substituteAll(storage.pythonList, listOf(pythonAnyType))
    val str = storage.pythonStr
    val bool = storage.pythonBool
    val float = storage.pythonFloat
    val dictOfAny = DefaultSubstitutionProvider.substituteAll(storage.pythonDict, listOf(pythonAnyType, pythonAnyType))
    val prefix = listOf(int, listOfAny, str, bool, float, dictOfAny)
    val rating = createTypeRating(
        storage.allTypes.filter {
            val description = it.pythonDescription()
            !description.name.name.startsWith("_")
                    && description is PythonConcreteCompositeTypeDescription
                    && !description.isAbstract
                    && !listOf("typing", "typing_extensions").any { mod -> description.name.prefix == listOf(mod) }
                    && !prefix.any { type -> typesAreEqual(type.getOrigin(), it) }
        },
        allLowerBounds,
        allUpperBounds,
        storage,
        1,
        withPenalty = false
    )
    return prefix + rating.types
}