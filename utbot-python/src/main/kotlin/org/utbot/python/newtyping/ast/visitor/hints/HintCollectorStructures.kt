package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.general.Type

class HintEdge(
    val from: HintCollectorNode,
    val to: HintCollectorNode,
    val dependency: (Type) -> (PartialTypeDescription) -> PartialTypeDescription
)

class HintCollectorNode(val typeDescription: PartialTypeDescription) {
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