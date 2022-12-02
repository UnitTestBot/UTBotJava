package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.*

class BaselineAlgorithm(val storage: PythonTypeStorage): TypeInferenceAlgorithm() {
    override fun run(hintCollectorResult: HintCollectorResult): Sequence<Type> = sequence {
        var state = getInitialState(hintCollectorResult)
        var nodeForExpansion = chooseNodeForExpansion(state)
        while (nodeForExpansion != null) {
            state = expandNode(state, nodeForExpansion)
            nodeForExpansion = chooseNodeForExpansion(state)
            yield(state.signature)
        }
    }

    // TODO: create rating, add new nodes for generics
    private fun chooseTypeForAny(node: AnyTypeNode): Type {
        val scores: Map<Type, Int> = storage.allTypes.associateWith { typeFromStorage ->
            val type = DefaultSubstitutionProvider.substituteAll(
                typeFromStorage,
                List(typeFromStorage.parameters.size) { pythonAnyType }
            )
            node.lowerBounds.fold(0) { acc, constraint ->
                acc + (if (PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, constraint, storage)) 1 else 0)
            }
            + node.upperBounds.fold(0) { acc, constraint ->
                acc + (if (PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(constraint, type, storage)) 1 else 0)
            }
        }
        return scores.maxBy { it.value }.key
    }

    // TODO: something smarter?
    private fun chooseNodeForExpansion(state: BaselineAlgorithmState): AnyTypeNode? {
        return state.nodes.find { it is AnyTypeNode }?.let { it as AnyTypeNode }
    }

    private fun expandNode(state: BaselineAlgorithmState, node: AnyTypeNode): BaselineAlgorithmState {
        assert(state.nodes.contains(node))
        val newType = chooseTypeForAny(node)
        val newNodeMap = expansionDFS(node, newType).toMutableMap()
        val newNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf()
        state.nodes.forEach { cur -> newNodeMap[cur] = newNodeMap[cur] ?: cur.copy() }
        state.nodes.forEach { cur ->
            val new = newNodeMap[cur]!!
            newNodes.add(new)
            cur.outgoingEdges.forEach { edge ->
                val newEdge = BaselineAlgorithmEdge(
                    from = new,
                    to = newNodeMap[edge.to]!!,
                    annotationParameterId = edge.annotationParameterId
                )
                addEdge(newEdge)
            }
        }
        return BaselineAlgorithmState(newNodes)
    }

    private fun expansionDFS(
        oldNode: BaselineAlgorithmNode,
        newType: Type,
    ): Map<BaselineAlgorithmNode, BaselineAlgorithmNode> {
        val newNode = PartialTypeNode(newType, oldNode.isRoot)
        val result: MutableMap<BaselineAlgorithmNode, BaselineAlgorithmNode> = mutableMapOf(oldNode to newNode)
        oldNode.outgoingEdges.forEach { edge ->
            val params = edge.to.partialType.pythonAnnotationParameters().toMutableList()
            params[edge.annotationParameterId] = newType
            val type = edge.to.partialType.pythonDescription().createTypeWithNewAnnotationParameters(edge.to.partialType, params)
            result += expansionDFS(edge.to, type)
        }
        return result
    }

    private fun getInitialState(hintCollectorResult: HintCollectorResult): BaselineAlgorithmState {
        val signatureDescription = hintCollectorResult.initialSignature.pythonDescription() as PythonCallableTypeDescription
        val root = PartialTypeNode(hintCollectorResult.initialSignature, true)
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
        return BaselineAlgorithmState(allNodes)
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
        val root = PartialTypeNode(partialType, false)
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

class BaselineAlgorithmState(val nodes: Set<BaselineAlgorithmNode>) {
    val signature: Type
        get() = nodes.find { it.isRoot }!!.partialType
}

class BaselineAlgorithmEdge(
    override val from: BaselineAlgorithmNode,
    override val to: BaselineAlgorithmNode,
    override val annotationParameterId: Int
): TypeInferenceEdgeWithValue

sealed class BaselineAlgorithmNode(val isRoot: Boolean): TypeInferenceNode {
    override val ingoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    override val outgoingEdges: MutableList<BaselineAlgorithmEdge> = mutableListOf()
    abstract fun copy(): BaselineAlgorithmNode
}
class PartialTypeNode(override val partialType: Type, isRoot: Boolean): BaselineAlgorithmNode(isRoot) {
    override fun copy(): BaselineAlgorithmNode = PartialTypeNode(partialType, isRoot)
}
class AnyTypeNode(val lowerBounds: List<Type>, val upperBounds: List<Type>): BaselineAlgorithmNode(false) {
    override val partialType: Type = pythonAnyType
    override fun copy(): BaselineAlgorithmNode = AnyTypeNode(lowerBounds, upperBounds)
}