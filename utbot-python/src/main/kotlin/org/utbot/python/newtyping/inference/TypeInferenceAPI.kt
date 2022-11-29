package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorResult
import org.utbot.python.newtyping.general.Type

abstract class TypeInferenceAlgorithm {
    abstract fun run(hintCollectorResult: HintCollectorResult): Sequence<Type>
}

interface TypeInferenceNode {
    val partialType: Type
    val ingoingEdges: Collection<TypeInferenceEdge>
    val outgoingEdges: Collection<TypeInferenceEdge>
}

interface TypeInferenceEdge {
    val from: TypeInferenceNode
    val to: TypeInferenceNode
}

interface TypeInferenceEdgeWithValue: TypeInferenceEdge {
    val annotationParameterId: Int
}

interface TypeInferenceEdgeWithBound: TypeInferenceEdge {
    val boundType: BoundType
    val dependency: (Type) -> List<Type>
    enum class BoundType {
        Lower,
        Upper
    }
}

fun addEdge(edge: TypeInferenceEdge) {
    val fromEdgeStorage = edge.from.outgoingEdges
    val toEdgeStorage = edge.to.ingoingEdges
    if (fromEdgeStorage is MutableCollection)
        fromEdgeStorage.add(edge)
    if (toEdgeStorage is MutableCollection)
        toEdgeStorage.add(edge)
}
