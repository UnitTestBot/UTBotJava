package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.addEdge
import org.utbot.python.newtyping.inference.constructors.FakeClassStorage
import org.utbot.python.newtyping.inference.constructors.constructFakeClass
import org.utbot.python.newtyping.inference.constructors.fakeClassCanBeConstructed
import java.util.LinkedList
import java.util.Queue

data class ExpansionResult(
    val newStates: List<BaselineAlgorithmState>,
    val newType: FunctionType,
    val fakeClassStorage: FakeClassStorage
)

fun expandState(state: BaselineAlgorithmState, typeStorage: PythonTypeStorage): ExpansionResult? {
    if (state.anyNodes.isEmpty())
        return null
    val types = state.candidateGraph.getNext() ?: return null
    val fakeClassStorage = state.fakeClassStorage.copy()
    val modifiedTypes = types.map {
        val description = it.pythonDescription()
        if (fakeClassCanBeConstructed(
                it,
                typeStorage
            ) && it is CompositeType && description is PythonConcreteCompositeTypeDescription
        ) {
            val newClass = constructFakeClass(it, description, fakeClassStorage)
            DefaultSubstitutionProvider.substituteAll(newClass, newClass.parameters.map { pythonAnyType })
        } else
            it
    }
    val substitution = (state.anyNodes zip modifiedTypes).associate { it }
    return expandNodes(state, substitution, state.generalRating, typeStorage, fakeClassStorage)
}

private fun expandNodes(
    state: BaselineAlgorithmState,
    substitution: Map<AnyTypeNode, Type>,
    generalRating: List<Type>,
    storage: PythonTypeStorage,
    fakeClassStorage: FakeClassStorage
): ExpansionResult {
    val newAnyNodeMap: MutableMap<AnyTypeNode, BaselineAlgorithmNode> = mutableMapOf()
    val allNewNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf()
    val anyNodeChunks: MutableList<List<AnyTypeNode>> = mutableListOf()
    substitution.entries.forEach { (node, newType) ->
        val (newNodeForAny, additionalNodes) =
            decompose(newType, node.lowerBounds, node.upperBounds, node.nestedLevel, storage)
        newAnyNodeMap[node] = newNodeForAny
        allNewNodes.addAll(additionalNodes)
        anyNodeChunks.add(
            additionalNodes.mapNotNull { it as? AnyTypeNode }
        )
    }
    val newNodeMap = expansionBFS(substitution, newAnyNodeMap).toMutableMap()
    state.nodes.forEach { cur -> newNodeMap[cur] = newNodeMap[cur] ?: cur.copy() }
    state.nodes.forEach { cur ->
        val new = newNodeMap[cur]!!
        cur.outgoingEdges.forEach { edge ->
            val newEdge = BaselineAlgorithmEdge(
                from = new,
                to = newNodeMap[edge.to]!!,
                annotationParameterId = edge.annotationParameterId
            )
            addEdge(newEdge)
        }
    }
    val allNodes = newNodeMap.values + allNewNodes
    val chunks = anyNodeChunks.map {
        BaselineAlgorithmState(allNodes, generalRating, storage, fakeClassStorage, it)
    }
    val type = chunks.first().signature
    return ExpansionResult(chunks, type as FunctionType, fakeClassStorage)
}

private fun expansionBFS(
    substitution: Map<AnyTypeNode, Type>,
    nodeMap: Map<AnyTypeNode, BaselineAlgorithmNode>
): Map<BaselineAlgorithmNode, BaselineAlgorithmNode> {
    val queue: Queue<Pair<BaselineAlgorithmNode, Int>> = LinkedList(substitution.keys.map { Pair(it, 0) })
    val newParams: MutableMap<BaselineAlgorithmNode, MutableList<Type>> = mutableMapOf()
    val newNodes: MutableMap<BaselineAlgorithmNode, BaselineAlgorithmNode> = nodeMap.toMutableMap()
    val lastModified: MutableMap<BaselineAlgorithmNode, Int> = mutableMapOf()
    var timer = 0
    while (!queue.isEmpty()) {
        timer++
        val (oldNode, putTime) = queue.remove()
        if ((lastModified[oldNode] ?: 0) != putTime)
            continue
        if (!newNodes.keys.contains(oldNode)) {
            val oldType = oldNode.partialType
            val newType = if (substitution.keys.contains(oldNode))
                substitution[oldNode]!!
            else
                oldType.pythonDescription().createTypeWithNewAnnotationParameters(
                    oldType,
                    newParams[oldNode] ?: oldType.pythonAnnotationParameters()
                )
            newNodes[oldNode] = PartialTypeNode(newType, oldNode.isRoot)
        }
        val newType = newNodes[oldNode]!!.partialType
        oldNode.outgoingEdges.forEach { edge ->
            val params = newParams[edge.to] ?: edge.to.partialType.pythonAnnotationParameters().toMutableList()
            params[edge.annotationParameterId] = newType
            newParams[edge.to] = params
            lastModified[edge.to] = timer
            queue.add(Pair(edge.to, timer))
        }
    }
    return newNodes
}