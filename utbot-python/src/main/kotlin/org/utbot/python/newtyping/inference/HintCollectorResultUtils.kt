package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorNode
import org.utbot.python.newtyping.ast.visitor.hints.HintEdge
import org.utbot.python.newtyping.ast.visitor.hints.HintEdgeWithBound
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnyType

fun visitNodesByReverseEdges(
    node: HintCollectorNode,
    visited: MutableSet<HintCollectorNode>,
    edgePredicate: (HintEdge) -> Boolean
) {
    visited.add(node)
    node.ingoingEdges.forEach { edge ->
        if (edgePredicate(edge) && !visited.contains(edge.from))
            visitNodesByReverseEdges(edge.from, visited, edgePredicate)
    }
}

fun collectBoundsFromEdges(node: HintCollectorNode): Pair<List<UtType>, List<UtType>> {
    val lowerBounds: MutableList<UtType> = mutableListOf()
    val upperBounds: MutableList<UtType> = mutableListOf()
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

fun collectBoundsFromComponent(component: Collection<HintCollectorNode>): Pair<List<UtType>, List<UtType>> {
    val lowerBounds: MutableList<UtType> = mutableListOf()
    val upperBounds: MutableList<UtType> = mutableListOf()
    component.forEach { visitedNode ->
        val (lowerFromEdges, upperFromEdges) = collectBoundsFromEdges(visitedNode)
        lowerBounds.addAll(visitedNode.lowerBounds + lowerFromEdges + visitedNode.partialType)
        upperBounds.addAll(visitedNode.upperBounds + upperFromEdges + visitedNode.partialType)
    }
    return Pair(lowerBounds, upperBounds)
}