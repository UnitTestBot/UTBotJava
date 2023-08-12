package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.general.UtType

interface TypeInferenceNode {
    val partialType: UtType
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
    val dependency: (UtType) -> List<UtType>
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
