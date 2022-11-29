package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.*

object BaselineAlgorithm: TypeInferenceAlgorithm() {
    override fun run(hintCollectorResult: HintCollectorResult): Sequence<Type> {
        val initialState = getInitialState(hintCollectorResult)
        return emptySequence()  // TODO
    }

    private fun getInitialState(hintCollectorResult: HintCollectorResult): BaselineAlgorithmState {
        val signatureDescription = hintCollectorResult.initialSignature.pythonDescription() as PythonCallableTypeDescription
        val root = PartialTypeNode(hintCollectorResult.initialSignature)
        val allNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf(root)
        val argumentRootNodes = signatureDescription.argumentNames.map { hintCollectorResult.parameterToNode[it]!! }
        argumentRootNodes.forEachIndexed { index, node ->
            val visited: MutableSet<HintCollectorNode> = mutableSetOf()
            visitIdentificationNodes(node, visited)
            val lowerBounds: MutableList<Type> = mutableListOf()
            val upperBounds: MutableList<Type> = mutableListOf()
            visited.forEach { visitedNode ->
                val (lowerFromEdges, upperFromEdges) = collectBoundsFromEdges(visitedNode)
                lowerBounds.addAll(visitedNode.lowerBounds + lowerFromEdges + visitedNode.partialType)
                upperBounds.addAll(visitedNode.upperBounds + upperFromEdges + visitedNode.partialType)
            }
            val decomposed = decompose(node.partialType, lowerBounds, upperBounds)
            allNodes.addAll(decomposed.nodes)
            val edge = BaselineAlgorithmEdge(
                from = decomposed.root,
                to = root,
                annotationParameterId = index
            )
            addEdge(edge)
        }
        return BaselineAlgorithmState(allNodes, root)
    }

    private fun visitIdentificationNodes(
        node: HintCollectorNode,
        visited: MutableSet<HintCollectorNode>
    ) {
        visited.add(node)
        node.ingoingEdges.forEach { edge ->
            if (edge is HintEdgeWithBound && edge.source == EdgeSource.Identification && !visited.contains(edge.from))
                visitIdentificationNodes(edge.from, visited)
        }
    }

    private fun decompose(partialType: Type, lowerBounds: List<Type>, upperBounds: List<Type>): DecompositionResult {
        if (partialType == pythonAnyType) {
            val root = AnyTypeNode(lowerBounds.filter { it != pythonAnyType }, upperBounds.filter { it != pythonAnyType })
            return DecompositionResult(root, setOf(root))
        }
        val newNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf()
        val root = PartialTypeNode(partialType)
        newNodes.add(root)
        val children = partialType.pythonAnnotationParameters()
        val constraints: Map<Int, MutableList<TypeConstraint>> =
            List(children.size) { it }.associateWith { mutableListOf() }
        lowerBounds.forEach { boundType ->
            val cur = propagateConstraint(partialType, TypeConstraint(boundType, ConstraintKind.LowerBound))
            cur.forEach { constraints[it.key]!!.add(it.value) }
        }
        upperBounds.forEach { boundType ->
            val cur = propagateConstraint(partialType, TypeConstraint(boundType, ConstraintKind.UpperBound))
            cur.forEach { constraints[it.key]!!.add(it.value) }
        }
        constraints.forEach { (index, constraintList) ->
            val childLowerBounds: MutableList<Type> = mutableListOf()
            val childUpperBounds: MutableList<Type> = mutableListOf()
            constraintList.forEach { constraint ->
                when (constraint.kind) {
                    ConstraintKind.LowerBound -> childLowerBounds.add(constraint.type)
                    ConstraintKind.UpperBound -> childUpperBounds.add(constraint.type)
                    ConstraintKind.BothSided -> {
                        childLowerBounds.add(constraint.type)
                        childUpperBounds.add(constraint.type)
                    }
                }
            }
            val (childBaselineAlgorithmNode, nodes) = decompose(children[index], childLowerBounds, childUpperBounds)
            newNodes.addAll(nodes)
            val edge = BaselineAlgorithmEdge(
                from = childBaselineAlgorithmNode,
                to = root,
                annotationParameterId = index
            )
            addEdge(edge)
        }
        return DecompositionResult(root, newNodes)
    }

    private fun collectBoundsFromEdges(node: HintCollectorNode): Pair<List<Type>, List<Type>> {
        val lowerBounds: MutableList<Type> = mutableListOf()
        val upperBounds: MutableList<Type> = mutableListOf()
        node.ingoingEdges.forEach { edge ->
            if (edge !is HintEdgeWithBound)
                return@forEach
            val hints = edge.dependency(pythonAnyType)
            when (edge.boundType) {
                TypeInferenceEdgeWithBound.BoundType.Lower -> lowerBounds.addAll(hints)
                TypeInferenceEdgeWithBound.BoundType.Upper -> upperBounds.addAll(hints)
            }
        }
        return Pair(lowerBounds, upperBounds)
    }

    private data class DecompositionResult(
        val root: BaselineAlgorithmNode,
        val nodes: Set<BaselineAlgorithmNode>
    )
}

class BaselineAlgorithmState(
    val nodes: Set<BaselineAlgorithmNode>,
    val signature: PartialTypeNode
)

class BaselineAlgorithmEdge(
    override val from: BaselineAlgorithmNode,
    override val to: BaselineAlgorithmNode,
    override val annotationParameterId: Int
): TypeInferenceEdgeWithValue

sealed class BaselineAlgorithmNode: TypeInferenceNode {
    override val ingoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    override val outgoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
}
class PartialTypeNode(override val partialType: Type): BaselineAlgorithmNode()
class AnyTypeNode(val lowerBounds: List<Type>, val upperBounds: List<Type>): BaselineAlgorithmNode() {
    override val partialType: Type = pythonAnyType
}