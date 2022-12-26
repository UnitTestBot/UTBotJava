package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.addEdge
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonDescription

fun expandState(state: BaselineAlgorithmState, typeStorage: PythonTypeStorage): BaselineAlgorithmState? {
    if (state.anyNodes.isEmpty())
        return null
    val types = state.candidateGraph.getNext() ?: return null
    val map: MutableMap<AnyTypeNode, AnyTypeNode> = mutableMapOf()
    return (state.anyNodes zip types).fold(state) { newState, (anyTypeNode, newType) ->
        var mappedNode = anyTypeNode
        while (map[mappedNode] != null)
            mappedNode = map[mappedNode]!!
        val (newNewState, newMap) = expandNode(newState, mappedNode, newType, state.generalRating, typeStorage)
        newMap.forEach { map[it.key] = it.value }
        newNewState
    }
}

private fun expandNode(
    state: BaselineAlgorithmState,
    node: AnyTypeNode,
    newType: Type,
    generalRating: TypeRating,
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