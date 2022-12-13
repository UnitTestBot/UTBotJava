package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.getBoundedParameters
import org.utbot.python.newtyping.inference.*
import org.utbot.python.newtyping.runmypy.checkSuggestedSignatureWithDMypy
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.utils.PriorityCartesianProduct
import org.utbot.python.utils.TemporaryFileManager
import java.io.File

private val EDGES_TO_LINK = listOf(
    EdgeSource.Identification,
    EdgeSource.Assign,
    EdgeSource.OpAssign,
    EdgeSource.Operation
)

private const val MAX_NESTING = 3

private val logger = KotlinLogging.logger {}

class BaselineAlgorithm(
    val storage: PythonTypeStorage,
    val pythonPath: String,
    private val pythonMethodCopy: PythonMethod,
    val directoriesForSysPath: Set<String>,
    val moduleToImport: String,
    private val initialErrorNumber: Int
) : TypeInferenceAlgorithm() {
    override fun run(hintCollectorResult: HintCollectorResult, isCancelled: () -> Boolean): Sequence<Type> = sequence {
        val generalRating = createGeneralTypeRating(hintCollectorResult)
        val initialState = getInitialState(hintCollectorResult, generalRating)
        val states: MutableSet<BaselineAlgorithmState> = mutableSetOf(initialState)
        val fileForMypyRuns = TemporaryFileManager.assignTemporaryFile(tag = "mypy.py")
        val configFile = setConfigFile(directoriesForSysPath)
        while (states.isNotEmpty()) {

            if (isCancelled())
                break

            val state = chooseState(states)
            val newState = state.makeMove()
            if (newState != null) {
                logger.debug("Checking ${newState.signature.pythonTypeRepresentation()}")
                if (checkSignature(newState.signature as FunctionType, fileForMypyRuns, configFile)) {
                    yield(newState.signature)
                    states.add(newState)
                }
            } else {
                states.remove(state)
            }
        }
    }

    private fun checkSignature(signature: FunctionType, fileForMypyRuns: File, configFile: File): Boolean {
        pythonMethodCopy.type = signature
        //print("checking: ")
        //println(signature.pythonTypeRepresentation())
        return checkSuggestedSignatureWithDMypy(
            pythonMethodCopy,
            directoriesForSysPath,
            moduleToImport,
            fileForMypyRuns,
            pythonPath,
            configFile,
            initialErrorNumber
        )
    }

    // TODO: something smarter?
    private fun chooseState(states: Set<BaselineAlgorithmState>): BaselineAlgorithmState {
        return states.random()
    }

    private fun createGeneralTypeRating(hintCollectorResult: HintCollectorResult): List<Type> {
        val allLowerBounds: MutableList<Type> = mutableListOf()
        val allUpperBounds: MutableList<Type> = mutableListOf()
        hintCollectorResult.allNodes.forEach { node ->
            val (lowerFromEdges, upperFromEdges) = collectBoundsFromEdges(node)
            allLowerBounds.addAll((lowerFromEdges + node.lowerBounds + node.partialType).filter {
                !equalTypes(it, pythonAnyType)
            })
            allUpperBounds.addAll((upperFromEdges + node.upperBounds + node.partialType).filter {
                !equalTypes(it, pythonAnyType)
            })
        }
        return createTypeRating(
            storage.allTypes.filter {
                val description = it.pythonDescription()
                !description.name.name.startsWith("_")
                        && description is PythonConcreteCompositeTypeDescription
                        && !description.isAbstract
            },
            allLowerBounds,
            allUpperBounds,
            storage,
            1
        )
    }

    private fun getInitialState(
        hintCollectorResult: HintCollectorResult,
        generalRating: List<Type>
    ): BaselineAlgorithmState {
        val signatureDescription =
            hintCollectorResult.initialSignature.pythonDescription() as PythonCallableTypeDescription
        val root = PartialTypeNode(hintCollectorResult.initialSignature, true)
        val allNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf(root)
        val argumentRootNodes = signatureDescription.argumentNames.map { hintCollectorResult.parameterToNode[it]!! }
        argumentRootNodes.forEachIndexed { index, node ->
            val visited: MutableSet<HintCollectorNode> = mutableSetOf()
            visitLinkedNodes(node, visited)
            val lowerBounds: MutableList<Type> = mutableListOf()
            val upperBounds: MutableList<Type> = mutableListOf()
            visited.forEach { visitedNode ->
                val (lowerFromEdges, upperFromEdges) = collectBoundsFromEdges(visitedNode)
                lowerBounds.addAll(visitedNode.lowerBounds + lowerFromEdges + visitedNode.partialType)
                upperBounds.addAll(visitedNode.upperBounds + upperFromEdges + visitedNode.partialType)
            }
            val decomposed = decompose(node.partialType, lowerBounds, upperBounds, 1)
            allNodes.addAll(decomposed.nodes)
            val edge = BaselineAlgorithmEdge(
                from = decomposed.root,
                to = root,
                annotationParameterId = index
            )
            addEdge(edge)
        }
        return BaselineAlgorithmState(allNodes, generalRating, storage)
    }

    private fun visitLinkedNodes(
        node: HintCollectorNode,
        visited: MutableSet<HintCollectorNode>
    ) {
        visited.add(node)
        node.ingoingEdges.forEach { edge ->
            if (edge is HintEdgeWithBound && EDGES_TO_LINK.contains(edge.source) && !visited.contains(edge.from))
                visitLinkedNodes(edge.from, visited)
        }
    }

    private fun collectBoundsFromEdges(node: HintCollectorNode): Pair<List<Type>, List<Type>> {
        val lowerBounds: MutableList<Type> = mutableListOf()
        val upperBounds: MutableList<Type> = mutableListOf()
        node.ingoingEdges.forEach { edge ->
            if (edge !is HintEdgeWithBound)
                return@forEach
            val hints = edge.dependency(pythonAnyType)
            when (edge.boundType) {
                TypeInferenceEdgeWithBound.BoundType.Lower -> lowerBounds.addAll(hints)
                TypeInferenceEdgeWithBound.BoundType.Upper -> upperBounds.addAll(hints)
            }
        }
        return Pair(lowerBounds, upperBounds)
    }
}

class BaselineAlgorithmState(
    val nodes: Set<BaselineAlgorithmNode>,
    private val generalRating: List<Type>,
    private val typeStorage: PythonTypeStorage
) {
    val signature: Type
        get() = nodes.find { it.isRoot }!!.partialType
    private val anyNodes: List<AnyTypeNode> = nodes.mapNotNull { it as? AnyTypeNode }
    private val anyNodesTypeRatings: List<List<Type>> =
        anyNodes.map {
            createTypeRating(generalRating, it.lowerBounds, it.upperBounds, typeStorage, it.nestedLevel)
        }
    private val typeChoices = PriorityCartesianProduct(anyNodesTypeRatings).getSequence().iterator()
    fun makeMove(): BaselineAlgorithmState? {
        if (anyNodes.isEmpty() || !typeChoices.hasNext())
            return null
        val types = typeChoices.next()
        val map: MutableMap<AnyTypeNode, AnyTypeNode> = mutableMapOf()
        return (anyNodes zip types).fold(this) { state, (anyTypeNode, newType) ->
            var mappedNode = anyTypeNode
            while (map[mappedNode] != null)
                mappedNode = map[mappedNode]!!
            val (newState, newMap) = expandNode(state, mappedNode, newType, generalRating, typeStorage)
            newMap.forEach { map[it.key] = it.value }
            newState
        }
    }
}

class BaselineAlgorithmEdge(
    override val from: BaselineAlgorithmNode,
    override val to: BaselineAlgorithmNode,
    override val annotationParameterId: Int
) : TypeInferenceEdgeWithValue

sealed class BaselineAlgorithmNode(val isRoot: Boolean) : TypeInferenceNode {
    override val ingoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    override val outgoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    abstract fun copy(): BaselineAlgorithmNode
}

class PartialTypeNode(override val partialType: Type, isRoot: Boolean) : BaselineAlgorithmNode(isRoot) {
    override fun copy(): BaselineAlgorithmNode = PartialTypeNode(partialType, isRoot)
}

class AnyTypeNode(val lowerBounds: List<Type>, val upperBounds: List<Type>, val nestedLevel: Int) :
    BaselineAlgorithmNode(false) {
    override val partialType: Type = pythonAnyType
    override fun copy(): BaselineAlgorithmNode = AnyTypeNode(lowerBounds, upperBounds, nestedLevel)
}

private fun equalTypes(left: Type, right: Type): Boolean =
    PythonTypeWrapperForEqualityCheck(left) == PythonTypeWrapperForEqualityCheck(right)

private fun createTypeRating(
    initialRating: List<Type>,
    lowerBounds: List<Type>,
    upperBounds: List<Type>,
    storage: PythonTypeStorage,
    level: Int
): List<Type> {
    val eps = 1
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
        Pair(type, (hintScores[wrapper] ?: 0.0) - type.getBoundedParameters().size * eps * level)
    }
    return scores.toList().sortedBy { -it.second }.map { it.first }
}

private fun expandNode(
    state: BaselineAlgorithmState,
    node: AnyTypeNode,
    newType: Type,
    generalRating: List<Type>,
    storage: PythonTypeStorage
): Pair<BaselineAlgorithmState, Map<AnyTypeNode, AnyTypeNode>> {
    assert(state.nodes.contains(node))
    val (newNodeForAny, additionalNodes) = decompose(newType, node.lowerBounds, node.upperBounds, node.nestedLevel)
    val newNodeMap = expansionDFS(node, newType, newNodeForAny).toMutableMap()
    val newNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf()
    state.nodes.forEach { cur -> newNodeMap[cur] = newNodeMap[cur] ?: cur.copy() }
    state.nodes.forEach { cur ->
        val new = newNodeMap[cur]!!
        newNodes.add(new)
        cur.outgoingEdges.forEach { edge ->
            val newEdge = BaselineAlgorithmEdge(
                from = new,
                to = newNodeMap[edge.to]!!,
                annotationParameterId = edge.annotationParameterId
            )
            addEdge(newEdge)
        }
    }
    return Pair(
        BaselineAlgorithmState(newNodes + additionalNodes, generalRating, storage),
        newNodeMap.mapNotNull {
            if (it.key !is AnyTypeNode || it.value !is AnyTypeNode) null else Pair(
                it.key as AnyTypeNode,
                it.value as AnyTypeNode
            )
        }.associate { it }
    )
}

private fun decompose(
    partialType: Type,
    lowerBounds: List<Type>,
    upperBounds: List<Type>,
    level: Int
): DecompositionResult {
    if (equalTypes(partialType, pythonAnyType)) {
        val root = AnyTypeNode(
            lowerBounds.filter { !equalTypes(it, pythonAnyType) },
            upperBounds.filter { !equalTypes(it, pythonAnyType) },
            level
        )
        return DecompositionResult(root, setOf(root))
    }
    val newNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf()
    val root = PartialTypeNode(partialType, false)
    newNodes.add(root)
    val children = partialType.pythonAnnotationParameters()
    val constraints: Map<Int, MutableList<TypeConstraint>> =
        List(children.size) { it }.associateWith { mutableListOf() }
    lowerBounds.forEach { boundType ->
        val cur = propagateConstraint(partialType, TypeConstraint(boundType, ConstraintKind.LowerBound))
        cur.forEach { constraints[it.key]!!.add(it.value) }
    }
    upperBounds.forEach { boundType ->
        val cur = propagateConstraint(partialType, TypeConstraint(boundType, ConstraintKind.UpperBound))
        cur.forEach { constraints[it.key]!!.add(it.value) }
    }
    constraints.forEach { (index, constraintList) ->
        val childLowerBounds: MutableList<Type> = mutableListOf()
        val childUpperBounds: MutableList<Type> = mutableListOf()
        constraintList.forEach { constraint ->
            when (constraint.kind) {
                ConstraintKind.LowerBound -> childLowerBounds.add(constraint.type)
                ConstraintKind.UpperBound -> childUpperBounds.add(constraint.type)
                ConstraintKind.BothSided -> {
                    childLowerBounds.add(constraint.type)
                    childUpperBounds.add(constraint.type)
                }
            }
        }
        val (childBaselineAlgorithmNode, nodes) = decompose(
            children[index],
            childLowerBounds,
            childUpperBounds,
            level + 1
        )
        newNodes.addAll(nodes)
        val edge = BaselineAlgorithmEdge(
            from = childBaselineAlgorithmNode,
            to = root,
            annotationParameterId = index
        )
        addEdge(edge)
    }
    return DecompositionResult(root, newNodes)
}

private fun expansionDFS(
    oldNode: BaselineAlgorithmNode,
    newType: Type,
    newNodeOpt: BaselineAlgorithmNode? = null
): Map<BaselineAlgorithmNode, BaselineAlgorithmNode> {
    val newNode = newNodeOpt ?: PartialTypeNode(newType, oldNode.isRoot)
    val result: MutableMap<BaselineAlgorithmNode, BaselineAlgorithmNode> = mutableMapOf(oldNode to newNode)
    oldNode.outgoingEdges.forEach { edge ->
        val params = edge.to.partialType.pythonAnnotationParameters().toMutableList()
        params[edge.annotationParameterId] = newType
        val type = edge.to.partialType.pythonDescription()
            .createTypeWithNewAnnotationParameters(edge.to.partialType, params)
        result += expansionDFS(edge.to, type)
    }
    return result
}

private data class DecompositionResult(
    val root: BaselineAlgorithmNode,
    val nodes: Set<BaselineAlgorithmNode>
)