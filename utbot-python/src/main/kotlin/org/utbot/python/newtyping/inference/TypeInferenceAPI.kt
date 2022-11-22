package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.PythonTypeWrapperForEqualityCheck
import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorResult
import org.utbot.python.newtyping.general.Type

open class TypeInferenceEdge(
    val from: TypeInferenceNode,
    val to: TypeInferenceNode,
    val dependency: (Type) -> (TypeRestrictionSet) -> TypeRestrictionSet
)

abstract class TypeRestriction

abstract class TypeRestrictionSet

abstract class TypeInferenceNode(
    val ingoingEdges: Set<TypeInferenceEdge>,
    val outgoingEdges: Set<TypeInferenceEdge>,
    val restrictions: TypeRestrictionSet
)

abstract class TypeInferenceState {
    abstract val currentFunctionSignature: Type
    abstract val nodes: Set<TypeInferenceNode>
}

abstract class TypeInferenceAlgorithm<NODE : TypeInferenceNode, STATE : TypeInferenceState> {
    fun run(initialSignature: Type, hintCollectorResult: HintCollectorResult): Sequence<Type> {
        val initialState = getInitialState(hintCollectorResult)
        val states = setOf(initialState)
        val foundSignatures: MutableSet<PythonTypeWrapperForEqualityCheck> =
            mutableSetOf(PythonTypeWrapperForEqualityCheck(initialSignature))
        TODO()
    }
    abstract fun getInitialState(hintCollectorResult: HintCollectorResult): STATE
    abstract fun chooseState(states: Set<STATE>): STATE
    abstract fun chooseNode(state: STATE): NODE
    abstract fun expandNode(node: NODE): NodeExpansionResult<NODE>
    class NodeExpansionResult<NODE: TypeInferenceNode>(
        val type: Type,
        val newNodes: Set<NODE>
    )
}