package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithValue
import org.utbot.python.newtyping.inference.TypeInferenceNode
import org.utbot.python.newtyping.pythonAnyType

sealed class BaselineAlgorithmNode(val isRoot: Boolean) : TypeInferenceNode {
    override val ingoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    override val outgoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    abstract fun copy(): BaselineAlgorithmNode
}

class PartialTypeNode(override val partialType: Type, isRoot: Boolean) : BaselineAlgorithmNode(isRoot) {
    override fun copy(): BaselineAlgorithmNode = PartialTypeNode(partialType, isRoot)
}

class AnyTypeNode(val lowerBounds: List<Type>, val upperBounds: List<Type>, val nestedLevel: Int) :
    BaselineAlgorithmNode(false) {
    override val partialType: Type = pythonAnyType
    override fun copy(): BaselineAlgorithmNode = AnyTypeNode(lowerBounds, upperBounds, nestedLevel)
}

class BaselineAlgorithmEdge(
    override val from: BaselineAlgorithmNode,
    override val to: BaselineAlgorithmNode,
    override val annotationParameterId: Int
) : TypeInferenceEdgeWithValue

class BaselineAlgorithmState(
    val nodes: Set<BaselineAlgorithmNode>,
    val generalRating: List<Type>,
    typeStorage: PythonTypeStorage
) {
    val signature: Type
        get() = nodes.find { it.isRoot }!!.partialType
    val anyNodes: List<AnyTypeNode> = nodes.mapNotNull { it as? AnyTypeNode }
    val candidateGraph = CandidateGraph(anyNodes, generalRating, typeStorage)
}