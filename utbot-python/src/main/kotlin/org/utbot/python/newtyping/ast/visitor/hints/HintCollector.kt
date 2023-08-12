package org.utbot.python.newtyping.ast.visitor.hints

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.*
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.*
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.TypeInferenceEdgeWithBound
import org.utbot.python.newtyping.inference.addEdge
import org.utbot.python.newtyping.mypy.GlobalNamesStorage
import java.util.*

class HintCollector(
    private val function: PythonFunctionDefinition,
    private val storage: PythonTypeHintsStorage,
    private val mypyTypes: Map<Pair<Int, Int>, UtType>,
    private val globalNamesStorage: GlobalNamesStorage,
    private val moduleOfSources: String
) : Collector() {
    private val parameterToNode: Map<String, HintCollectorNode> =
        (function.meta.args.map { it.name } zip function.type.arguments).associate {
            it.first to HintCollectorNode(it.second)
        }
    private val astNodeToHintCollectorNode: MutableMap<Node, HintCollectorNode> = mutableMapOf()
    private val identificationToNode: MutableMap<Block?, MutableMap<String, HintCollectorNode>> = mutableMapOf()
    private val blockStack = Stack<Block>()

    init {
        val argNames = function.meta.args.map { it.name }
        assert(argNames.all { it != "" })
        identificationToNode[null] = mutableMapOf()
        argNames.forEach {
            identificationToNode[null]!![it] = parameterToNode[it]!!
        }
    }

    lateinit var result: HintCollectorResult
    override fun finishCollection() {
        val allNodes: MutableSet<HintCollectorNode> = mutableSetOf()
        (parameterToNode + astNodeToHintCollectorNode).forEach {
            if (!allNodes.contains(it.value))
                collectAllNodes(it.value, allNodes)
        }
        result = HintCollectorResult(parameterToNode, function.type, allNodes)
    }

    private fun collectAllNodes(cur: HintCollectorNode, visited: MutableSet<HintCollectorNode>) {
        visited.add(cur)
        cur.outgoingEdges.forEach {
            val to = it.to
            if (!visited.contains(to))
                collectAllNodes(to, visited)
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
            is FunctionCall -> processFunctionCall(node)
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
            is Comparison -> processComparison(node)
            is org.parsers.python.ast.List -> processList(node)
            is Assignment -> processAssignment(node)
            is DotName -> processDotName(node)
            is SliceExpression -> processSliceExpression(node)
            // TODO: UnaryExpression, Power, ShiftExpression, BitwiseAnd, BitwiseOr, BitwiseXor
            // TODO: Set, Dict, comprehensions
            is Newline, is IndentToken, is Delimiter, is Operator, is DedentToken, is ReturnStatement,
            is Statement, is InvocationArguments, is Argument, is Slice, is Slices -> Unit
            else -> astNodeToHintCollectorNode[node] = HintCollectorNode(pythonAnyType)
        }
    }

    private fun processFunctionCall(node: FunctionCall) {
        val parsed = parseFunctionCall(node)
        if (parsed == null) {
            astNodeToHintCollectorNode[node] = HintCollectorNode(pythonAnyType)
            return
        }
        if (processIsinstanceCall(parsed, node))
            return
        val rawType = mypyTypes[parsed.function.beginOffset to parsed.function.endOffset]
        val attr = rawType?.getPythonAttributeByName(
            storage,
            "__call__"
        )
        val type = attr?.type
        val typeDescription = type?.pythonDescription()
        val callType = createPythonCallableType(
            parsed.args.size,
            List(parsed.args.size) { PythonCallableTypeDescription.ArgKind.ARG_POS },
            List(parsed.args.size) { "" }
        ) {
            FunctionTypeCreator.InitializationData(List(parsed.args.size) { pythonAnyType }, pythonAnyType)
        }
        when (typeDescription) {
            is PythonCallableTypeDescription -> {
                astNodeToHintCollectorNode[node] =
                    HintCollectorNode(typeDescription.castToCompatibleTypeApi(type).returnValue)
                if (parsed.args.size != typeDescription.numberOfArguments)
                    return
                (parsed.args zip typeDescription.castToCompatibleTypeApi(type).arguments).forEach { (argNode, bound) ->
                    astNodeToHintCollectorNode[argNode]!!.upperBounds.add(bound)
                }
            }
            is PythonOverloadTypeDescription -> {
                val hintNode = HintCollectorNode(pythonAnyType)
                astNodeToHintCollectorNode[node] = hintNode
                typeDescription.getAnnotationParameters(type).forEach { typeCandidate ->
                    val descr = typeCandidate.pythonDescription() as? PythonCallableTypeDescription ?: return@forEach
                    val typeCandidateFunctionType = descr.castToCompatibleTypeApi(typeCandidate)
                    if (!signaturesAreCompatible(typeCandidateFunctionType, callType, storage))
                        return@forEach
                    (parsed.args zip typeCandidateFunctionType.arguments).forEach { (argNode, argType) ->
                        astNodeToHintCollectorNode[argNode]!!.upperBounds.add(argType)
                    }
                    hintNode.upperBounds.add(typeCandidateFunctionType.returnValue)
                }
            }
            else -> {
                astNodeToHintCollectorNode[node] = HintCollectorNode(pythonAnyType)
                val funcNode = parsed.function
                astNodeToHintCollectorNode[funcNode]!!.upperBounds.add(
                    createCallableProtocol(List(parsed.args.size) { pythonAnyType }, pythonAnyType)
                )
            }
        }
    }

    private fun processIsinstanceCall(parsedFunctionCall: ParsedFunctionCall, node: FunctionCall): Boolean {
        if (parsedFunctionCall.function !is Name || parsedFunctionCall.function.toString() != "isinstance" ||
            parsedFunctionCall.args.size != 2)
            return false

        val typeAsString = parsedFunctionCall.args[1].toString()
        val type = globalNamesStorage.resolveTypeName(moduleOfSources, typeAsString) ?: return false

        astNodeToHintCollectorNode[node] = HintCollectorNode(storage.pythonBool)
        val objNode = astNodeToHintCollectorNode[parsedFunctionCall.args[0]]!!
        objNode.lowerBounds.add(type)
        return true
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
        val type = typeOfNumericalLiteral(node, storage)
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
        if (identificationToNode[block]!![name] == null) {
            val type = mypyTypes[node.beginOffset to node.endOffset] ?: pythonAnyType
            identificationToNode[block]!![name] = HintCollectorNode(type)
        }
        astNodeToHintCollectorNode[node] = identificationToNode[block]!![name]!!
    }

    private fun processForStatement(node: ForStatement) {
        val parsed = parseForStatement(node) ?: return
        // TODO: case of TupleForVariable
        val iterableNode = astNodeToHintCollectorNode[parsed.iterable]!!
        if (parsed.forVariable !is SimpleForVariable) {
            addProtocol(iterableNode, createIterableWithCustomReturn(pythonAnyType))
            return
        }
        val variableNode = astNodeToHintCollectorNode[parsed.forVariable.variable]!!
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
        processBinaryExpression(
            curNode,
            parsed.left,
            parsed.right,
            getOperationOfOperator(parsed.op.toString()) ?: return
        )
    }

    private fun processMultiplicativeExpression(node: MultiplicativeExpression) {
        val curNode = HintCollectorNode(pythonAnyType)
        astNodeToHintCollectorNode[node] = curNode
        val parsed = parseMultiplicativeExpression(node)
        parsed.cases.forEach {
            processBinaryExpression(
                curNode,
                it.left,
                it.right,
                getOperationOfOperator(it.op.toString()) ?: return,
                source = EdgeSource.Multiplication
            )
        }
    }

    private fun processComparison(node: Comparison) {
        val parsed = parseComparison(node)
        parsed.cases.forEach { (left, op, right) ->
            val comp = getComparison(op.toString()) ?: return@forEach
            val leftNode = astNodeToHintCollectorNode[left]!!
            val rightNode = astNodeToHintCollectorNode[right]!!

            val edgeFromLeftToRight = HintEdgeWithBound(
                from = leftNode,
                to = rightNode,
                source = EdgeSource.Comparison,
                boundType = TypeInferenceEdgeWithBound.BoundType.Upper
            ) { rightType -> listOf(createBinaryProtocol(comp.method, rightType, storage.pythonBool)) }
            val edgeFromRightToLeft = HintEdgeWithBound(
                from = rightNode,
                to = leftNode,
                source = EdgeSource.Comparison,
                boundType = TypeInferenceEdgeWithBound.BoundType.Upper
            ) { leftType -> listOf(createBinaryProtocol(reverseComparison(comp).method, leftType, storage.pythonBool)) }

            addEdge(edgeFromLeftToRight)
            addEdge(edgeFromRightToLeft)
        }
        processBoolExpression(node)
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
                targetNodes.forEach { target ->
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

    private fun processDotName(node: DotName) {
        val parsed = parseDotName(node)
        val type = mypyTypes[node.beginOffset to node.endOffset] ?: pythonAnyType
        val curNode = HintCollectorNode(type)
        astNodeToHintCollectorNode[node] = curNode
        val headNode = astNodeToHintCollectorNode[parsed.head]!!
        val attribute = parsed.tail.toString()
        // addProtocol(headNode, createProtocolWithAttribute(attribute, pythonAnyType))

        val edgeFromHead = HintEdgeWithBound(
            from = headNode,
            to = curNode,
            source = EdgeSource.Attribute,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { headType ->
            headType.getPythonAttributeByName(storage, attribute)?.let { listOf(it.type) } ?: emptyList()
        }
        val edgeToHead = HintEdgeWithBound(
            from = curNode,
            to = headNode,
            source = EdgeSource.Attribute,
            boundType = TypeInferenceEdgeWithBound.BoundType.Upper
        ) { curType ->
            listOf(createProtocolWithAttribute(attribute, curType))
        }
        addEdge(edgeFromHead)
        addEdge(edgeToHead)
    }

    private fun processSliceExpression(node: SliceExpression) {
        val curNode = HintCollectorNode(pythonAnyType)
        astNodeToHintCollectorNode[node] = curNode
        val parsed = parseSliceExpression(node) ?: return
        val headNode = astNodeToHintCollectorNode[parsed.head]!!
        when (parsed.slices) {
            is SimpleSlice -> {
                val indexNode = astNodeToHintCollectorNode[parsed.slices.indexValue]!!
                val edgeFromIndexToHead = HintEdgeWithBound(
                    from = indexNode,
                    to = headNode,
                    source = EdgeSource.Slice,
                    boundType = TypeInferenceEdgeWithBound.BoundType.Upper
                ) { indexType ->
                    listOf(createBinaryProtocol("__getitem__", indexType, pythonAnyType))
                }
                val edgeFromHeadToIndex = HintEdgeWithBound(
                    from = headNode,
                    to = indexNode,
                    source = EdgeSource.Slice,
                    boundType = TypeInferenceEdgeWithBound.BoundType.Upper
                ) { headType ->
                    val attr = headType.getPythonAttributeByName(storage, "__getitem__")
                        ?: return@HintEdgeWithBound emptyList()
                    // TODO: case of Overload
                    val descr = attr.type.pythonDescription() as? PythonCallableTypeDescription
                        ?: return@HintEdgeWithBound emptyList()
                    if (descr.numberOfArguments != 2)
                        return@HintEdgeWithBound emptyList()
                    listOf(attr.type.parameters[1])
                }
                addEdge(edgeFromIndexToHead)
                addEdge(edgeFromHeadToIndex)
            }
            is SlicedSlice -> {
                addProtocol(headNode, createBinaryProtocol("__getitem__", storage.pythonSlice, pythonAnyType))
                // not necessary, but usually. TODO: remove if it spoils anything
                for (slicePart in listOf(parsed.slices.start, parsed.slices.end, parsed.slices.step)) {
                    if (slicePart == null)
                        continue
                    val sliceNode = astNodeToHintCollectorNode[slicePart]!!
                    sliceNode.lowerBounds.add(storage.pythonInt)
                }
            }
            is TupleSlice -> {
                addProtocol(headNode, createBinaryProtocol("__getitem__", storage.tupleOfAny, pythonAnyType))
            }
        }
        val edgeFromCurToHead = HintEdgeWithBound(
            from = curNode,
            to = headNode,
            source = EdgeSource.Slice,
            boundType = TypeInferenceEdgeWithBound.BoundType.Upper
        ) { curType ->
            listOf(createBinaryProtocol("__getitem__", pythonAnyType, curType))
        }
        val edgeFromHeadToCur = HintEdgeWithBound(
            from = headNode,
            to = curNode,
            source = EdgeSource.Slice,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { headType ->
            val attr = headType.getPythonAttributeByName(storage, "__getitem__")
                ?: return@HintEdgeWithBound emptyList()
            // TODO: case of Overload
            val descr = attr.type.pythonDescription() as? PythonCallableTypeDescription
                ?: return@HintEdgeWithBound emptyList()
            listOf(descr.castToCompatibleTypeApi(attr.type).returnValue)
        }
        addEdge(edgeFromCurToHead)
        addEdge(edgeFromHeadToCur)
    }

    private fun processBoolExpression(node: Node) {
        val hintCollectorNode = HintCollectorNode(storage.pythonBool)
        astNodeToHintCollectorNode[node] = hintCollectorNode
    }

    private fun addProtocol(node: HintCollectorNode, protocol: UtType) {
        node.upperBounds.add(protocol)
    }

    private fun processBinaryExpression(
        curNode: HintCollectorNode,
        left: Node,
        right: Node,
        op: Operation,
        source: EdgeSource = EdgeSource.Operation
    ) {
        val leftNode = astNodeToHintCollectorNode[left]!!
        val rightNode = astNodeToHintCollectorNode[right]!!
        val methodName = op.method

        val edgeFromLeftToCur = HintEdgeWithBound(
            from = leftNode,
            to = curNode,
            source = source,
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
            source = source,
            boundType = TypeInferenceEdgeWithBound.BoundType.Lower
        ) { rightType ->
            listOf(
                (rightType.getPythonAttributeByName(storage, methodName)
                    ?: return@HintEdgeWithBound emptyList()).type
            )
        }
        addEdge(edgeFromLeftToCur)
        addEdge(edgeFromRightToCur)

        val edgeFromLeftToRight = HintEdgeWithBound(
            from = leftNode,
            to = rightNode,
            source = source,
            boundType = TypeInferenceEdgeWithBound.BoundType.Upper
        ) { leftType -> listOf(createBinaryProtocol(methodName, leftType, pythonAnyType)) }
        val edgeFromRightToLeft = HintEdgeWithBound(
            from = rightNode,
            to = leftNode,
            source = source,
            boundType = TypeInferenceEdgeWithBound.BoundType.Upper
        ) { rightType -> listOf(createBinaryProtocol(methodName, rightType, pythonAnyType)) }
        addEdge(edgeFromLeftToRight)
        addEdge(edgeFromRightToLeft)

        listOf(leftNode, rightNode).forEach {
            val edgeFromCurToUp = HintEdgeWithBound(
                from = curNode,
                to = it,
                source = source,
                boundType = TypeInferenceEdgeWithBound.BoundType.Upper
            ) { curType -> listOf(createBinaryProtocol(methodName, pythonAnyType, curType)) }
            addEdge(edgeFromCurToUp)
        }

    }
}