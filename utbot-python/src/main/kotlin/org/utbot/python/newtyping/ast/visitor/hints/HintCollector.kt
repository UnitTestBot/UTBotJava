package org.utbot.python.newtyping.ast.visitor.hints

import org.parsers.python.Node
import org.parsers.python.ast.ForStatement
import org.utbot.python.newtyping.ast.parseForStatement
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.pythonAnyType

class HintCollector(
    val parameters: List<FunctionParameter>
) : Collector() {
    val nodes: MutableSet<HintCollectorNode> = mutableSetOf()
    private val parameterToNode: Map<FunctionParameter, HintCollectorNode> =
        parameters.associateWith { HintCollectorNode(PartialTypeDescription(it.type, emptyList(), emptyList())) }
    private val astNodeToHintCollectorNode: MutableMap<Node, HintCollectorNode> = mutableMapOf()

    init {
        parameters.forEach {
            nodes.add(parameterToNode[it]!!)
        }
    }

    override fun collectFromNodeAfterRecursion(node: Node) {
        when (node) {
            is ForStatement -> processForStatement(node)
            else -> {
                astNodeToHintCollectorNode[node] =
                    HintCollectorNode(PartialTypeDescription(pythonAnyType, emptyList(), emptyList()))
            }
        }
    }

    private fun processForStatement(node: ForStatement) {
        val parsed = parseForStatement(node)
        val variableNode = astNodeToHintCollectorNode[parsed.forVariable]!!
        val iterableNode = astNodeToHintCollectorNode[parsed.iterable]!!
        val edgeFromVariableToIterable = HintEdge(
            from = variableNode,
            to = iterableNode
        ) { varType ->
            { iterableDescription ->
                PartialTypeDescription(
                    iterableDescription.partialType,
                    iterableDescription.lowerBounds,
                    iterableDescription.upperBounds + createIterableWithCustomReturn(varType)
                )
            }
        }
        val edgeFromIterableToVariable = HintEdge(
            from = iterableNode,
            to = variableNode
        ) { iterType ->
            { variableDescription ->
                val iterReturnType = iterType.getPythonAttributeByName("__iter__")?.type ?: pythonAnyType
                PartialTypeDescription(
                    variableDescription.partialType,
                    variableDescription.lowerBounds + listOf(iterReturnType),
                    variableDescription.upperBounds
                )
            }
        }
        variableNode.outgoingEdges.add(edgeFromVariableToIterable)
        iterableNode.ingoingEdges.add(edgeFromVariableToIterable)
        variableNode.ingoingEdges.add(edgeFromIterableToVariable)
        iterableNode.outgoingEdges.add(edgeFromIterableToVariable)
    }
}