package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.addEdge
import org.utbot.python.newtyping.inference.constructors.FakeClassStorage
import org.utbot.python.newtyping.inference.constructors.constructFakeClass
import org.utbot.python.newtyping.inference.constructors.fakeClassCanBeConstructed
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonDescription
import java.util.LinkedList
import java.util.Queue

fun expandState(
    state: BaselineAlgorithmState,
    typeStorage: PythonTypeStorage,
    constructFakeClasses: Boolean
): BaselineAlgorithmState? {
    if (state.anyNodes.isEmpty())
        return null
    val types = state.candidateGraph.getNext() ?: return null
    val fakeClassStorage = state.fakeClassStorage.copy()
    val modifiedTypes = types.map { type ->
        val description = type.pythonDescription()
        if (constructFakeClasses && fakeClassCanBeConstructed(
                type,
                typeStorage
            ) && description is PythonConcreteCompositeTypeDescription
        ) {
            constructFakeClass(description.castToCompatibleTypeApi(type), description, fakeClassStorage)
        } else {
            type
        }
    }
    return expandState(state, typeStorage, modifiedTypes, fakeClassStorage)
}

fun expandState(
    state: BaselineAlgorithmState,
    typeStorage: PythonTypeStorage,
    types: List<Type>,
    fakeClassStorage: FakeClassStorage? = null
): BaselineAlgorithmState? {
    if (types.isEmpty())
        return null
    val substitution = (state.anyNodes zip types).associate { it }
    return expandNodes(
        state,
        substitution,
        state.generalRating,
        typeStorage,
        fakeClassStorage ?: state.fakeClassStorage.copy()
    )
}

private fun expandNodes(
    state: BaselineAlgorithmState,
    substitution: Map<AnyTypeNode, Type>,
    generalRating: List<Type>,
    storage: PythonTypeStorage,
    fakeClassStorage: FakeClassStorage
): BaselineAlgorithmState {
    val (newAnyNodeMap, allNewNodes) = substitution.entries.fold(
        Pair(emptyMap<AnyTypeNode, BaselineAlgorithmNode>(), emptySet<BaselineAlgorithmNode>())
    ) { (map, allNodeSet), (node, newType) ->
        val (newNodeForAny, additionalNodes) =
            decompose(newType, node.lowerBounds, node.upperBounds, node.nestedLevel, storage)
        Pair(map + (node to newNodeForAny), allNodeSet + additionalNodes)
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
    return BaselineAlgorithmState(
        newNodeMap.values.toSet() + allNewNodes,
        generalRating,
        storage,
        fakeClassStorage
    )
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