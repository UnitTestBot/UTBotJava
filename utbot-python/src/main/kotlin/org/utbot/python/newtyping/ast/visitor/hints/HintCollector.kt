package org.utbot.python.newtyping.ast.visitor.hints

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.*
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.*
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithBound
import org.utbot.python.newtyping.inference.addEdge
import java.util.*

class HintCollector(val signature: FunctionType, val storage: PythonTypeStorage) : Collector() {
    private val signatureDescription = signature.pythonDescription() as PythonCallableTypeDescription
    private val parameterToNode: Map<String, HintCollectorNode> =
        (signatureDescription.argumentNames zip signature.arguments).associate {
            it.first to HintCollectorNode(it.second)
        }
    private val astNodeToHintCollectorNode: MutableMap<Node, HintCollectorNode> = mutableMapOf()
    private val identificationToNode: MutableMap<Block?, MutableMap<String, HintCollectorNode>> = mutableMapOf()
    private val blockStack = Stack<Block>()

    init {
        val argNames = signatureDescription.argumentNames
        assert(argNames.all { it != "" })
        identificationToNode[null] = mutableMapOf()
        argNames.forEach {
            identificationToNode[null]!![it] = parameterToNode[it]!!
        }
    }

    lateinit var result: HintCollectorResult
    override fun finishCollection() {
        result = HintCollectorResult(parameterToNode, signature)
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
            is MultiplicativeExpression -> processMultiplicativeExpression(node)
            is org.parsers.python.ast.List -> processList(node)
            is Assignment -> processAssignment(node)
            // TODO: UnaryExpression, Power, ShiftExpression, BitwiseAnd, BitwiseOr, BitwiseXor
            // TODO: Set, Dict, comprehensions
            // TODO: DotName, FunctionCall, SliceExpression
            is Newline, is IndentToken, is Delimiter, is Operator, is DedentToken, is ReturnStatement,
            is Statement, is InvocationArguments -> Unit
            else -> astNodeToHintCollectorNode[node] = HintCollectorNode(pythonAnyType)
        }
    }

    private fun processKeyword(node: Keyword) {
        val type = when (node.type) {
            PythonConstants.TokenType.TRUE, PythonConstants.TokenType.FALSE -> storage.pythonBool
            PythonConstants.TokenType.NONE -> pythonNoneType
            PythonConstants.TokenType.FOR, PythonConstants.TokenType.IF,
            PythonConstants.TokenType.ELSE, PythonConstants.TokenType.ELIF,
            PythonConstants.TokenType.WHILE, PythonConstants.TokenType.IN,
            PythonConstants.TokenType.RETURN, PythonConstants.TokenType.OR, PythonConstants.TokenType.AND,
            PythonConstants.TokenType.NOT -> return
            else -> pythonAnyType
        }
        astNodeToHintCollectorNode[node] = HintCollectorNode(type)
    }

    private fun processNumericalLiteral(node: NumericalLiteral) {
        val type = when (node.type) {
            PythonConstants.TokenType.DECNUMBER,
            PythonConstants.TokenType.HEXNUMBER,
            PythonConstants.TokenType.OCTNUMBER -> storage.pythonInt
            PythonConstants.TokenType.FLOAT -> storage.pythonFloat
            PythonConstants.TokenType.COMPLEX -> storage.pythonComplex
            else -> pythonAnyType
        }
        astNodeToHintCollectorNode[node] = HintCollectorNode(type)
    }

    private fun processStringLiteral(node: StringLiteral) {
        astNodeToHintCollectorNode[node] = HintCollectorNode(storage.pythonStr)
    }

    private fun processBlock(node: Block) {
        blockStack.pop()
        val prevBlock = if (blockStack.isEmpty()) null else blockStack.peek()
        identificationToNode[node]!!.forEach { (id, hintNode) ->
            val prevHintNode = identificationToNode[prevBlock]!![id] ?: HintCollectorNode(pythonAnyType)
            identificationToNode[prevBlock]!![id] = prevHintNode
            val edgeFromPrev = HintEdgeWithBound(
                from = prevHintNode,
                to = hintNode,
                source = EdgeSource.Identification,
                boundType = TypeInferenceEdgeWithBound.BoundType.Upper
            ) { upperType -> listOf(upperType) }
            val edgeToPrev = HintEdgeWithBound(
                from = hintNode,
                to = prevHintNode,
                source = EdgeSource.Identification,
                boundType = TypeInferenceEdgeWithBound.BoundType.Lower
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
        val hintNode = identificationToNode[block]!![name] ?: HintCollectorNode(pythonAnyType)
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
            boundType = TypeInferenceEdgeWithBound.BoundType.Upper
        ) { varType -> listOf(createIterableWithCustomReturn(varType)) }
        val edgeFromIterableToVariable = HintEdgeWithBound(
            from = iterableNode,
            to = variableNode,
            source = EdgeSource.ForStatement,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { iterType ->
            val iterReturnType = iterType.getPythonAttributeByName(storage, "__iter__")?.type
                ?: return@HintEdgeWithBound emptyList()
            listOf(iterReturnType)
        }

        addEdge(edgeFromVariableToIterable)
        addEdge(edgeFromIterableToVariable)
    }

    private fun processIfStatement(node: IfStatement) {
        val parsed = parseIfStatement(node)
        addProtocol(astNodeToHintCollectorNode[parsed.condition]!!, supportsBoolProtocol(storage))
    }

    private fun processConjunction(node: Conjunction) {
        processBoolExpression(node)
        val parsed = parseConjunction(node)
        addProtocol(astNodeToHintCollectorNode[parsed.left]!!, supportsBoolProtocol(storage))
        addProtocol(astNodeToHintCollectorNode[parsed.right]!!, supportsBoolProtocol(storage))
    }

    private fun processDisjunction(node: Disjunction) {
        processBoolExpression(node)
        val parsed = parseDisjunction(node)
        addProtocol(astNodeToHintCollectorNode[parsed.left]!!, supportsBoolProtocol(storage))
        addProtocol(astNodeToHintCollectorNode[parsed.right]!!, supportsBoolProtocol(storage))
    }

    private fun processInversion(node: Inversion) {
        processBoolExpression(node)
        val parsed = parseInversion(node)
        addProtocol(astNodeToHintCollectorNode[parsed.expr]!!, supportsBoolProtocol(storage))
    }

    private fun processGroup(node: Group) {
        val parsed = parseGroup(node)
        val exprNode = astNodeToHintCollectorNode[parsed.expr]!!
        astNodeToHintCollectorNode[node] = exprNode
    }

    private fun processAdditiveExpression(node: AdditiveExpression) {
        val curNode = HintCollectorNode(pythonAnyType)
        astNodeToHintCollectorNode[node] = curNode
        val parsed = parseAdditiveExpression(node) ?: return
        processBinaryExpression(curNode, parsed.left, parsed.right, getOperationOfOperator(parsed.op.toString()) ?: return)
    }

    private fun processMultiplicativeExpression(node: MultiplicativeExpression) {
        val curNode = HintCollectorNode(pythonAnyType)
        astNodeToHintCollectorNode[node] = curNode
        val parsed = parseMultiplicativeExpression(node) ?: return
        processBinaryExpression(curNode, parsed.left, parsed.right, getOperationOfOperator(parsed.op.toString()) ?: return)
    }

    private fun processList(node: org.parsers.python.ast.List) {
        val parsed = parseList(node)
        val partialType = DefaultSubstitutionProvider.substituteByIndex(storage.pythonList, 0, pythonAnyType)
        val curNode = HintCollectorNode(partialType)
        astNodeToHintCollectorNode[node] = curNode
        val innerTypeNode = HintCollectorNode(pythonAnyType)
        val edgeFromInnerToCur = HintEdgeWithValue(
            from = innerTypeNode,
            to = curNode,
            annotationParameterId = 0
        )
        addEdge(edgeFromInnerToCur)

        parsed.elems.forEach { elem ->
            val elemNode = astNodeToHintCollectorNode[elem]!!
            val edge = HintEdgeWithBound(
                from = elemNode,
                to = innerTypeNode,
                source = EdgeSource.CollectionElement,
                boundType = TypeInferenceEdgeWithBound.BoundType.Lower
            ) { listOf(it) }
            addEdge(edge)
        }
    }

    private fun processAssignment(node: Assignment) {
        val parsed = parseAssignment(node) ?: return
        when (parsed) {
            is SimpleAssign -> {
                val targetNodes = parsed.targets.map { astNodeToHintCollectorNode[it]!! }
                val valueNode = astNodeToHintCollectorNode[parsed.value]!!
                targetNodes.forEach {  target ->
                    val edgeFromValue = HintEdgeWithBound(
                        from = valueNode,
                        to = target,
                        source = EdgeSource.Assign,
                        boundType = TypeInferenceEdgeWithBound.BoundType.Lower
                    ) { listOf(it) }
                    val edgeFromTarget = HintEdgeWithBound(
                        from = target,
                        to = valueNode,
                        source = EdgeSource.Assign,
                        boundType = TypeInferenceEdgeWithBound.BoundType.Upper
                    ) { listOf(it) }
                    addEdge(edgeFromValue)
                    addEdge(edgeFromTarget)
                }
            }
            is OpAssign -> {
                val targetNode = astNodeToHintCollectorNode[parsed.target]!!
                val op = getOperationOfOpAssign(parsed.op.toString()) ?: return
                val nodeForOpResult = HintCollectorNode(pythonAnyType)
                processBinaryExpression(nodeForOpResult, parsed.target, parsed.value, op)

                val edgeToTarget = HintEdgeWithBound(
                    from = nodeForOpResult,
                    to = targetNode,
                    source = EdgeSource.OpAssign,
                    boundType = TypeInferenceEdgeWithBound.BoundType.Lower
                ) { listOf(it) }
                val edgeFromTarget = HintEdgeWithBound(
                    from = targetNode,
                    to = nodeForOpResult,
                    source = EdgeSource.OpAssign,
                    boundType = TypeInferenceEdgeWithBound.BoundType.Upper
                ) { listOf(it) }

                addEdge(edgeToTarget)
                addEdge(edgeFromTarget)
            }
        }
    }

    private fun processBoolExpression(node: Node) {
        val hintCollectorNode = HintCollectorNode(storage.pythonBool)
        astNodeToHintCollectorNode[node] = hintCollectorNode
    }

    private fun addProtocol(node: HintCollectorNode, protocol: Type) {
        node.upperBounds.add(protocol)
    }

    private fun processBinaryExpression(curNode: HintCollectorNode, left: Node, right: Node, op: Operation) {
        val leftNode = astNodeToHintCollectorNode[left]!!
        val rightNode = astNodeToHintCollectorNode[right]!!
        val methodName = op.method
        addProtocol(leftNode, createBinaryProtocol(methodName, pythonAnyType, pythonAnyType))
        addProtocol(rightNode, createBinaryProtocol(methodName, pythonAnyType, pythonAnyType))

        val edgeFromLeftToCur = HintEdgeWithBound(
            from = leftNode,
            to = curNode,
            source = EdgeSource.Operation,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { leftType ->
            listOf(
                (leftType.getPythonAttributeByName(storage, methodName)
                    ?: return@HintEdgeWithBound emptyList()).type
            )
        }
        val edgeFromRightToCur = HintEdgeWithBound(
            from = rightNode,
            to = curNode,
            source = EdgeSource.Operation,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { rightType ->
            listOf(
                (rightType.getPythonAttributeByName(storage, methodName)
                    ?: return@HintEdgeWithBound emptyList()).type
            )
        }
        addEdge(edgeFromLeftToCur)
        addEdge(edgeFromRightToCur)

        // TODO: other type dependencies (from cur to left and right, from right to left, from left to right)
    }
}