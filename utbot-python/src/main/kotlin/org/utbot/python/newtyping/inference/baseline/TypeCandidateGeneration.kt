package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorResult
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.getBoundedParameters
import org.utbot.python.newtyping.inference.collectBoundsFromEdges

private val logger = KotlinLogging.logger {}

class TypeRating(
    scores: List<Pair<Type, Double>>
) {
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
        get() =
            if (pos == 0) 0.0 else typeRating.typesInOrder[pos - 1].first - typeRating.typesInOrder.first().first
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
        positions.map { pos ->
            CandidateGraphVertex(
                positions.map {
                    if (it == pos) pos.makeMove() else it
                }
            )
        }

    val types by lazy {
        positions.map { it.type }
    }
}

private const val MAX_NESTING = 3

fun createTypeRating(
    initialRating: List<Type>,
    lowerBounds: List<Type>,
    upperBounds: List<Type>,
    storage: PythonTypeStorage,
    level: Int
): TypeRating {
    val hintScores = mutableMapOf<PythonTypeWrapperForEqualityCheck, Double>()
    lowerBounds.forEach { constraint ->
        val fitting = initialRating.filter { typeFromList ->
            val type = DefaultSubstitutionProvider.substitute(
                typeFromList,
                typeFromList.getBoundedParameters().associateWith { pythonAnyType }
            )
            PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, constraint, storage)
        }
        fitting.forEach {
            val wrapper = PythonTypeWrapperForEqualityCheck(it)
            hintScores[wrapper] = (hintScores[wrapper] ?: 0.0) + 1.0 / fitting.size
        }
    }
    upperBounds.forEach { constraint ->
        val fitting = initialRating.filter { typeFromList ->
            val type = DefaultSubstitutionProvider.substitute(
                typeFromList,
                typeFromList.getBoundedParameters().associateWith { pythonAnyType }
            )
            PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(constraint, type, storage)
        }
        if (fitting.isNotEmpty())
            fitting.forEach {
                val wrapper = PythonTypeWrapperForEqualityCheck(it)
                hintScores[wrapper] = (hintScores[wrapper] ?: 0.0) + 1.0 / fitting.size
            }
    }
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
    val result = TypeRating(scores)
    //logger.debug("Generated type rating: " +
    //        "${result.typesInOrder.take(15).map { Pair(it.second.pythonTypeRepresentation(), it.first) }}")
    return result
}

fun createGeneralTypeRating(hintCollectorResult: HintCollectorResult, storage: PythonTypeStorage): TypeRating {
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
    return createTypeRating(
        storage.allTypes.filter {
            val description = it.pythonDescription()
            !description.name.name.startsWith("_")
                    && description is PythonConcreteCompositeTypeDescription
                    && !description.isAbstract
                    && description.name.prefix != listOf("typing")
        },
        allLowerBounds,
        allUpperBounds,
        storage,
        1
    )
}