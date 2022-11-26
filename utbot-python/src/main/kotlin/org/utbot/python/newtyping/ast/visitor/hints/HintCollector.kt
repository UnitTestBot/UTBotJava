package org.utbot.python.newtyping.ast.visitor.hints

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.*
import org.utbot.python.newtyping.BuiltinTypes
import org.utbot.python.newtyping.ast.*
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.pythonNoneType
import java.util.*

class HintCollector(
    val parameters: List<FunctionParameter>
) : Collector() {
    private val parameterToNode: Map<FunctionParameter, HintCollectorNode> =
        parameters.associateWith { HintCollectorNode(PartialTypeDescription(it.type)) }
    private val astNodeToHintCollectorNode: MutableMap<Node, HintCollectorNode> = mutableMapOf()
    private val identificationToNode: MutableMap<Block?, MutableMap<String, HintCollectorNode>> = mutableMapOf()
    private val blockStack = Stack<Block>()

    init {
        identificationToNode[null] = mutableMapOf()
        parameters.forEach {
            identificationToNode[null]!![it.name] = parameterToNode[it]!!
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
            is Keyword -> processKeyword(node)
            is NumericalLiteral -> processNumericalLiteral(node)
            is StringLiteral -> processStringLiteral(node)
            is Block -> processBlock(node)
            is Name -> processName(node)
            is ForStatement -> processForStatement(node)
            is IfStatement -> processIfStatement(node)
            is Conjunction -> processConjunction(node)
            is Disjunction -> processDisjunction(node)
            is Inversion -> processInversion(node)
            is Group -> processGroup(node)
            is AdditiveExpression -> processAdditiveExpression(node)
            is org.parsers.python.ast.List -> processList(node)
            // TODO: Assignment, MultiplicativeExpression, UnaryExpression, Power, ShiftExpression, BitwiseAnd, BitwiseOr, BitwiseXor
            // TODO: Set, Dict, comprehensions
            // TODO: DotName, FunctionCall, SliceExpression
            is Newline, is IndentToken, is Delimiter, is Operator, is DedentToken, is ReturnStatement,
            is Assignment, is Statement, is InvocationArguments -> Unit
            else -> astNodeToHintCollectorNode[node] = HintCollectorNode(PartialTypeDescription(pythonAnyType))
        }
    }

    private fun processKeyword(node: Keyword) {
        val type = when (node.toString()) {
            "True", "False" -> BuiltinTypes.pythonBool
            "None" -> pythonNoneType
            "for", "if", "else", "elif", "while", "in", "return", "or", "and", "not" -> return
            else -> pythonAnyType
        }
        astNodeToHintCollectorNode[node] = HintCollectorNode(PartialTypeDescription(type))
    }

    private fun processNumericalLiteral(node: NumericalLiteral) {
        val type = when (node.type) {
            PythonConstants.TokenType.DECNUMBER,
            PythonConstants.TokenType.HEXNUMBER,
            PythonConstants.TokenType.OCTNUMBER -> BuiltinTypes.pythonInt
            PythonConstants.TokenType.FLOAT -> BuiltinTypes.pythonFloat
            PythonConstants.TokenType.COMPLEX -> BuiltinTypes.pythonComplex
            else -> pythonAnyType
        }
        astNodeToHintCollectorNode[node] = HintCollectorNode(PartialTypeDescription(type))
    }

    private fun processStringLiteral(node: StringLiteral) {
        astNodeToHintCollectorNode[node] = HintCollectorNode(PartialTypeDescription(BuiltinTypes.pythonStr))
    }

    private fun processBlock(node: Block) {
        blockStack.pop()
        val prevBlock = if (blockStack.isEmpty()) null else blockStack.peek()
        identificationToNode[node]!!.forEach { (id, hintNode) ->
            val prevHintNode = identificationToNode[prevBlock]!![id] ?: HintCollectorNode(
                PartialTypeDescription(pythonAnyType)
            )
            identificationToNode[prevBlock]!![id] = prevHintNode
            val edgeFromPrev = HintEdgeWithBound(
                from = prevHintNode,
                to = hintNode,
                source = EdgeSource.Identification,
                boundType = HintEdgeWithBound.BoundType.Upper
            ) { upperType -> listOf(upperType) }
            val edgeToPrev = HintEdgeWithBound(
                from = hintNode,
                to = prevHintNode,
                source = EdgeSource.Identification,
                boundType = HintEdgeWithBound.BoundType.Lower
            ) { lowerType -> listOf(lowerType) }

            addEdge(edgeFromPrev)
            addEdge(edgeToPrev)
        }
    }

    private fun processName(node: Name) {
        if (!isIdentification(node))
            return
        val name = node.toString()
        val block = blockStack.peek()
        val hintNode = identificationToNode[block]!![name] ?: HintCollectorNode(PartialTypeDescription(pythonAnyType))
        identificationToNode[block]!![name] = hintNode
        astNodeToHintCollectorNode[node] = hintNode
    }

    private fun processForStatement(node: ForStatement) {
        val parsed = parseForStatement(node)
        val variableNode = astNodeToHintCollectorNode[parsed.forVariable]!!
        val iterableNode = astNodeToHintCollectorNode[parsed.iterable]!!
        val edgeFromVariableToIterable = HintEdgeWithBound(
            from = variableNode,
            to = iterableNode,
            source = EdgeSource.ForStatement,
            boundType = HintEdgeWithBound.BoundType.Upper
        ) { varType -> listOf(createIterableWithCustomReturn(varType)) }
        val edgeFromIterableToVariable = HintEdgeWithBound(
            from = iterableNode,
            to = variableNode,
            source = EdgeSource.ForStatement,
            boundType = HintEdgeWithBound.BoundType.Lower
        ) { iterType ->
            val iterReturnType = iterType.getPythonAttributeByName("__iter__")?.type
                ?: return@HintEdgeWithBound emptyList()
            listOf(iterReturnType)
        }

        addEdge(edgeFromVariableToIterable)
        addEdge(edgeFromIterableToVariable)
    }

    private fun processIfStatement(node: IfStatement) {
        val parsed = parseIfStatement(node)
        addProtocol(astNodeToHintCollectorNode[parsed.condition]!!, supportsBoolProtocol)
    }

    private fun processConjunction(node: Conjunction) {
        processBoolExpression(node)
        val parsed = parseConjunction(node)
        addProtocol(astNodeToHintCollectorNode[parsed.left]!!, supportsBoolProtocol)
        addProtocol(astNodeToHintCollectorNode[parsed.right]!!, supportsBoolProtocol)
    }

    private fun processDisjunction(node: Disjunction) {
        processBoolExpression(node)
        val parsed = parseDisjunction(node)
        addProtocol(astNodeToHintCollectorNode[parsed.left]!!, supportsBoolProtocol)
        addProtocol(astNodeToHintCollectorNode[parsed.right]!!, supportsBoolProtocol)
    }

    private fun processInversion(node: Inversion) {
        processBoolExpression(node)
        val parsed = parseInversion(node)
        addProtocol(astNodeToHintCollectorNode[parsed.expr]!!, supportsBoolProtocol)
    }

    private fun processGroup(node: Group) {
        val parsed = parseGroup(node)
        val exprNode = astNodeToHintCollectorNode[parsed.expr]!!
        astNodeToHintCollectorNode[node] = exprNode
    }

    private fun processAdditiveExpression(node: AdditiveExpression) {
        val parsed = parseAdditiveExpression(node)
        val leftNode = astNodeToHintCollectorNode[parsed.left]!!
        val rightNode = astNodeToHintCollectorNode[parsed.right]!!
        val curNode = HintCollectorNode(PartialTypeDescription(pythonAnyType))
        astNodeToHintCollectorNode[node] = curNode
        val methodName = operationToMagicMethod(parsed.op.toString()) ?: return
        addProtocol(leftNode, createBinaryProtocol(methodName, pythonAnyType, pythonAnyType))
        addProtocol(rightNode, createBinaryProtocol(methodName, pythonAnyType, pythonAnyType))

        val edgeFromLeftToCur = HintEdgeWithBound(
            from = leftNode,
            to = curNode,
            source = EdgeSource.Operation,
            boundType = HintEdgeWithBound.BoundType.Lower
        ) { leftType ->
            listOf((leftType.getPythonAttributeByName(methodName) ?: return@HintEdgeWithBound emptyList()).type)
        }
        val edgeFromRightToCur = HintEdgeWithBound(
            from = rightNode,
            to = curNode,
            source = EdgeSource.Operation,
            boundType = HintEdgeWithBound.BoundType.Lower
        ) { rightType ->
            listOf((rightType.getPythonAttributeByName(methodName) ?: return@HintEdgeWithBound emptyList()).type)
        }

        addEdge(edgeFromLeftToCur)
        addEdge(edgeFromRightToCur)

        // TODO: other type dependencies (from cur to left and right, from right to left, from left to right)
    }

    private fun processList(node: org.parsers.python.ast.List) {
        val parsed = parseList(node)
        val typeDescription = PartialTypeDescription(
            DefaultSubstitutionProvider.substituteByIndex(BuiltinTypes.pythonList, 0, pythonAnyType)
        )
        val curNode = HintCollectorNode(typeDescription)
        astNodeToHintCollectorNode[node] = curNode
        val innerTypeNode = HintCollectorNode(PartialTypeDescription(pythonAnyType))
        val edgeFromInnerToCur = HintEdgeWithValue(
            from = innerTypeNode,
            to = curNode
        ) { innerType, _ -> DefaultSubstitutionProvider.substituteByIndex(BuiltinTypes.pythonList, 0, innerType) }
        addEdge(edgeFromInnerToCur)

        parsed.elems.forEach { elem ->
            val elemNode = astNodeToHintCollectorNode[elem]!!
            val edge = HintEdgeWithBound(
                from = elemNode,
                to = innerTypeNode,
                source = EdgeSource.CollectionElement,
                boundType = HintEdgeWithBound.BoundType.Lower
            ) { listOf(it) }
            addEdge(edge)
        }
    }

    private fun processBoolExpression(node: Node) {
        val typeDescription = PartialTypeDescription(BuiltinTypes.pythonBool)
        val hintCollectorNode = HintCollectorNode(typeDescription)
        astNodeToHintCollectorNode[node] = hintCollectorNode
    }

    private fun addProtocol(node: HintCollectorNode, protocol: Type) {
        node.typeDescription.upperBounds.add(protocol)
    }
}