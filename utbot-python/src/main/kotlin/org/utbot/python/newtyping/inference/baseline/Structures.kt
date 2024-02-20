package org.utbot.python.newtyping.inference.baseline

import org.utpython.types.PythonTypeHintsStorage
import org.utpython.types.general.UtType
import org.utpython.types.inference.TypeInferenceEdgeWithValue
import org.utpython.types.inference.TypeInferenceNode
import org.utpython.types.pythonAnyType

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
    typeStorage: PythonTypeHintsStorage,
    val additionalVars: String = "",
) {
    val signature: UtType
        get() = nodes.find { it.isRoot }!!.partialType
    val anyNodes: List<AnyTypeNode> = nodes.mapNotNull { it as? AnyTypeNode }
    val candidateGraph = CandidateGraph(anyNodes, generalRating, typeStorage)
    var children: Int = 0
}