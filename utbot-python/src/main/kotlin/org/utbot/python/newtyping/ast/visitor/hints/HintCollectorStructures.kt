package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.TypeInferenceEdge
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithBound
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithValue
import org.utbot.python.newtyping.inference.TypeInferenceNode

sealed class HintEdge(
    override val from: HintCollectorNode,
    override val to: HintCollectorNode,
): TypeInferenceEdge

class HintEdgeWithBound(
    from: HintCollectorNode,
    to: HintCollectorNode,
    val source: EdgeSource,
    override val boundType: TypeInferenceEdgeWithBound.BoundType,
    override val dependency: (UtType) -> List<UtType>
): HintEdge(from, to), TypeInferenceEdgeWithBound

class HintEdgeWithValue(
    from: HintCollectorNode,
    to: HintCollectorNode,
    override val annotationParameterId: Int
): HintEdge(from, to), TypeInferenceEdgeWithValue

class HintCollectorNode(override val partialType: UtType): TypeInferenceNode {
    val lowerBounds: MutableList<UtType> = mutableListOf()
    val upperBounds: MutableList<UtType> = mutableListOf()
    override val outgoingEdges: MutableSet<HintEdge> = mutableSetOf()
    override val ingoingEdges: MutableSet<HintEdge> = mutableSetOf()
}

class HintCollectorResult(
    val parameterToNode: Map<String, HintCollectorNode>,
    val initialSignature: FunctionType,
    val allNodes: Set<HintCollectorNode>
)

enum class EdgeSource {
    ForStatement,
    Identification,
    Operation,
    Multiplication,
    CollectionElement,
    Assign,
    OpAssign,
    Attribute,
    Comparison,
    Slice
}