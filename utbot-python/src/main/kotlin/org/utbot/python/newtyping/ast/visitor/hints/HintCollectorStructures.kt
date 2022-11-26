package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonAnyType

fun addEdge(edge: HintEdge) {
    edge.from.outgoingEdges.add(edge)
    edge.to.ingoingEdges.add(edge)
}

sealed class HintEdge(
    val from: HintCollectorNode,
    val to: HintCollectorNode,
)

class HintEdgeWithBound(
    from: HintCollectorNode,
    to: HintCollectorNode,
    val source: EdgeSource,
    val boundType: BoundType,
    val dependency: (Type) -> List<Type>
): HintEdge(from, to) {
    enum class BoundType {
        Lower,
        Upper
    }
}

class HintEdgeWithValue(
    from: HintCollectorNode,
    to: HintCollectorNode,
    val dependency: (Type, Type) -> Type  // (newTypeOfFrom, oldTypeOfTo) -> newTypeOfTo
): HintEdge(from, to)

class HintCollectorNode(val typeDescription: PartialTypeDescription) {
    val outgoingEdges: MutableSet<HintEdge> = mutableSetOf()
    val ingoingEdges: MutableSet<HintEdge> = mutableSetOf()
}

class PartialTypeDescription(val partialType: Type) {
    val lowerBounds: MutableList<Type> = mutableListOf()
    val upperBounds: MutableList<Type> = mutableListOf()
}

class HintCollectorResult

class FunctionParameter(
    val name: String,
    val type: Type
)

enum class EdgeSource {
    ForStatement,
    Group,
    Identification,
    Operation,
    CollectionElement
}