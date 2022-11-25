package org.utbot.python.newtyping.ast.visitor.hints

import org.parsers.python.Node
import org.parsers.python.ast.*
import org.utbot.python.newtyping.BuiltinTypes
import org.utbot.python.newtyping.ast.*
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.pythonAnyType
import java.util.*

class HintCollector(
    val parameters: List<FunctionParameter>
) : Collector() {
    val nodes: MutableSet<HintCollectorNode> = mutableSetOf()
    private val parameterToNode: Map<FunctionParameter, HintCollectorNode> =
        parameters.associateWith { HintCollectorNode() }
    private val astNodeToHintCollectorNode: MutableMap<Node, HintCollectorNode> = mutableMapOf()
    private val identificationToNode: MutableMap<Block?, MutableMap<String, HintCollectorNode>> = mutableMapOf()
    private val blockStack = Stack<Block>()

    init {
        identificationToNode[null] = mutableMapOf()
        parameters.forEach {
            nodes.add(parameterToNode[it]!!)
            val node = HintCollectorNode()
            identificationToNode[null]!![it.name] = node
            node.typeDescription = PartialTypeDescription(it.type, emptyList(), emptyList())
        }
    }

    override fun collectFromNodeBeforeRecursion(node: Node) {
        if (node is Block) {
            blockStack.add(node)
            identificationToNode[node] = mutableMapOf()
        }
    }

    override fun collectFromNodeAfterRecursion(node: Node) {
        when (node) {
            is Block -> processBlock(node)
            is Name -> processName(node)
            is ForStatement -> processForStatement(node)
            is IfStatement -> processIfStatement(node)
            is Conjunction -> processConjunction(node)
            is Disjunction -> processDisjunction(node)
            is Inversion -> processInversion(node)
            is Group -> processGroup(node)
            // is DotName
            else -> astNodeToHintCollectorNode[node] = HintCollectorNode()
        }
    }

    private fun processBlock(node: Block) {
        blockStack.pop()
        val prevBlock = if (blockStack.isEmpty()) null else blockStack.peek()
        identificationToNode[node]!!.forEach { (id, hintNode) ->
            val prevHintNode = identificationToNode[prevBlock]!![id] ?: HintCollectorNode()
            identificationToNode[prevBlock]!![id] = prevHintNode
            val edgeFromPrev = HintEdge(
                from = prevHintNode,
                to = hintNode,
                source = EdgeSource.Identification
            ) { upperType ->
                { description ->
                    PartialTypeDescription(
                        description.partialType,
                        description.lowerBounds,
                        description.upperBounds + listOf(upperType)
                    )
                }
            }
            val edgeToPrev = HintEdge(
                from = hintNode,
                to = prevHintNode,
                source = EdgeSource.Identification
            ) { lowerType ->
                { description ->
                    PartialTypeDescription(
                        description.partialType,
                        description.lowerBounds + listOf(lowerType),
                        description.upperBounds
                    )
                }
            }
            prevHintNode.outgoingEdges.add(edgeFromPrev)
            prevHintNode.ingoingEdges.add(edgeToPrev)
            hintNode.outgoingEdges.add(edgeToPrev)
            hintNode.ingoingEdges.add(edgeFromPrev)
        }
    }

    private fun processName(node: Name) {
        if (!isIdentification(node))
            return
        val name = node.toString()
        val block = blockStack.peek()
        val hintNode = identificationToNode[block]!![name] ?: HintCollectorNode()
        identificationToNode[block]!![name] = hintNode
        astNodeToHintCollectorNode[node] = hintNode
    }

    private fun processForStatement(node: ForStatement) {
        val parsed = parseForStatement(node)
        val variableNode = astNodeToHintCollectorNode[parsed.forVariable]!!
        val iterableNode = astNodeToHintCollectorNode[parsed.iterable]!!
        val edgeFromVariableToIterable = HintEdge(
            from = variableNode,
            to = iterableNode,
            source = EdgeSource.ForStatement
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
            to = variableNode,
            source = EdgeSource.ForStatement
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

    private fun processIfStatement(node: IfStatement) {
        val parsed = parseIfStatement(node)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.condition]!!)
    }

    private fun processConjunction(node: Conjunction) {
        processBoolExpression(node)
        val parsed = parseConjunction(node)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.left]!!)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.right]!!)
    }

    private fun processDisjunction(node: Disjunction) {
        processBoolExpression(node)
        val parsed = parseDisjunction(node)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.left]!!)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.right]!!)
    }

    private fun processInversion(node: Inversion) {
        processBoolExpression(node)
        val parsed = parseInversion(node)
        addBoolProtocol(astNodeToHintCollectorNode[parsed.expr]!!)
    }

    private fun processGroup(node: Group) {
        val parsed = parseGroup(node)
        val exprNode = astNodeToHintCollectorNode[parsed.expr]!!
        val curNode = HintCollectorNode()
        astNodeToHintCollectorNode[node] = curNode
        val dependency: (Type) -> (PartialTypeDescription) -> (PartialTypeDescription) = { type ->
            { description ->
                PartialTypeDescription(type, description.lowerBounds, description.upperBounds)
            }
        }
        val edgeOut = HintEdge(from = exprNode, to = curNode, EdgeSource.Group, dependency)
        val edgeIn = HintEdge(from = curNode, to = exprNode, EdgeSource.Group, dependency)
        curNode.outgoingEdges.add(edgeIn)
        exprNode.ingoingEdges.add(edgeIn)
        curNode.ingoingEdges.add(edgeOut)
        exprNode.outgoingEdges.add(edgeOut)
    }

    private fun processBoolExpression(node: Node) {
        val hintCollectorNode = HintCollectorNode()
        hintCollectorNode.typeDescription = PartialTypeDescription(
            BuiltinTypes.pythonBool,
            emptyList(),
            emptyList()
        )
        astNodeToHintCollectorNode[node] = hintCollectorNode
    }

    private fun addBoolProtocol(node: HintCollectorNode) {
        val oldDescription = node.typeDescription
        node.typeDescription = PartialTypeDescription(
            oldDescription.partialType,
            oldDescription.lowerBounds,
            oldDescription.upperBounds + listOf(supportsBoolProtocol)
        )
    }
}