package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonAnyType

class HintEdge(
    val from: HintCollectorNode,
    val to: HintCollectorNode,
    val source: EdgeSource,
    val dependency: (Type) -> (PartialTypeDescription) -> PartialTypeDescription
)

class HintCollectorNode {
    var typeDescription: PartialTypeDescription = PartialTypeDescription(pythonAnyType, emptyList(), emptyList())
    val outgoingEdges: MutableSet<HintEdge> = mutableSetOf()
    val ingoingEdges: MutableSet<HintEdge> = mutableSetOf()
}

class PartialTypeDescription(
    val partialType: Type,
    val lowerBounds: List<Type>,
    val upperBounds: List<Type>
)

class HintCollectorResult

class FunctionParameter(
    val name: String,
    val type: Type
)

enum class EdgeSource {
    ForStatement,
    Group
}