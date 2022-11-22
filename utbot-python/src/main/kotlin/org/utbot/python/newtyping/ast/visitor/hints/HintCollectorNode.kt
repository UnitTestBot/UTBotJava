package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.general.Type

class HintEdge(
    val from: UndefinedType,
    val to: UndefinedType,
    val dependency: (Type) -> Type
)

sealed class HintCollectorNode

class UndefinedType: HintCollectorNode() {
    val outgoingEdges: MutableSet<HintEdge> = mutableSetOf()
    val ingoingEdges: MutableSet<HintEdge> = mutableSetOf()
}

class KnownType(val type: Type): HintCollectorNode()