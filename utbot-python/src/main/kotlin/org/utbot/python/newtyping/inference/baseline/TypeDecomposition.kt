package org.utbot.python.newtyping.inference.baseline

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.addEdge

data class DecompositionResult(
    val root: BaselineAlgorithmNode,
    val nodes: Set<BaselineAlgorithmNode>
)

fun decompose(
    partialType: Type,
    lowerBounds: List<Type>,
    upperBounds: List<Type>,
    level: Int
): DecompositionResult {
    if (typesAreEqual(partialType, pythonAnyType)) {
        val root = AnyTypeNode(
            lowerBounds.filter { !typesAreEqual(it, pythonAnyType) },
            upperBounds.filter { !typesAreEqual(it, pythonAnyType) },
            level
        )
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
        val (childBaselineAlgorithmNode, nodes) = decompose(
            children[index],
            childLowerBounds,
            childUpperBounds,
            level + 1
        )
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