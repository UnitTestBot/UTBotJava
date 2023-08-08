package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithValue
import org.utbot.python.newtyping.inference.TypeInferenceNode
import org.utbot.python.newtyping.pythonAnyType

sealed class BaselineAlgorithmNode(val isRoot: Boolean) : TypeInferenceNode {
    override val ingoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    override val outgoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    abstract fun copy(): BaselineAlgorithmNode
}

class PartialTypeNode(override val partialType: UtType, isRoot: Boolean) : BaselineAlgorithmNode(isRoot) {
    override fun copy(): BaselineAlgorithmNode = PartialTypeNode(partialType, isRoot)
}

class AnyTypeNode(val lowerBounds: List<UtType>, val upperBounds: List<UtType>, val nestedLevel: Int) :
    BaselineAlgorithmNode(false) {
    override val partialType: UtType = pythonAnyType
    override fun copy(): BaselineAlgorithmNode = AnyTypeNode(lowerBounds, upperBounds, nestedLevel)
}

class BaselineAlgorithmEdge(
    override val from: BaselineAlgorithmNode,
    override val to: BaselineAlgorithmNode,
    override val annotationParameterId: Int
) : TypeInferenceEdgeWithValue

class BaselineAlgorithmState(
    val nodes: Set<BaselineAlgorithmNode>,
    val generalRating: List<UtType>,
    typeStorage: PythonTypeStorage
) {
    val signature: UtType
        get() = nodes.find { it.isRoot }!!.partialType
    val anyNodes: List<AnyTypeNode> = nodes.mapNotNull { it as? AnyTypeNode }
    val candidateGraph = CandidateGraph(anyNodes, generalRating, typeStorage)
    var children: Int = 0
}