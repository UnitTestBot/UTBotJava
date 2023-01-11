package org.utbot.python.newtyping.ast

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.*

data class ParsedFunctionDefinition(val name: Name, val body: Block)
data class ParsedForStatement(val forVariable: ForVariable, val iterable: Node)
sealed class ForVariable
data class SimpleForVariable(val variable: Name) : ForVariable()
data class TupleForVariable(val elems: List<ForVariable>) : ForVariable()
data class ParsedSliceExpression(val head: Node, val slices: ParsedSlices)
sealed class ParsedSlices
data class SimpleSlice(val indexValue: Node) : ParsedSlices()
data class SlicedSlice(val start: Node?, val end: Node?, val step: Node?) : ParsedSlices()
data class TupleSlice(val elems: List<ParsedSlices>) : ParsedSlices()
data class ParsedIfStatement(val condition: Node)
data class ParsedConjunction(val left: Node, val right: Node)
data class ParsedDisjunction(val left: Node, val right: Node)
data class ParsedInversion(val expr: Node)
data class ParsedGroup(val expr: Node)
data class ParsedList(val elems: List<Node>)
sealed class ParsedAssignment
data class SimpleAssign(val targets: List<Node>, val value: Node) : ParsedAssignment()
data class OpAssign(val target: Node, val op: Delimiter, val value: Node) : ParsedAssignment()
data class ParsedMultiplicativeExpression(val cases: List<ParsedBinaryOperation>)
data class ParsedBinaryOperation(val left: Node, val op: Node, val right: Node)
data class ParsedDotName(val head: Node, val tail: Node)
data class ParsedFunctionCall(val function: Node, val args: List<Node>)
data class ParsedComparison(val cases: List<PrimitiveComparison>)
data class PrimitiveComparison(val left: Node, val op: Delimiter, val right: Node)

fun parseFunctionDefinition(node: FunctionDefinition): ParsedFunctionDefinition? {
    val name = (node.children().first { it is Name } ?: return null) as Name
    val body = (node.children().find { it is Block } ?: return null) as Block
    return ParsedFunctionDefinition(name, body)
}

fun isIdentification(node: Node): Boolean {
    val name = node as? Name ?: return false
    val parent = name.parent as? DotName ?: return true
    return parent.children().first() == node
}

fun parseForVariable(node: Node): ForVariable? {
    if (node is Name)
        return SimpleForVariable(node)
    if (node is Tuple) {
        return TupleForVariable(
            node.children().mapNotNull { child ->
                if (child is Delimiter)
                    return@mapNotNull null
                parseForVariable(child)
            }
        )
    }
    return null
}

fun parseForStatement(node: ForStatement): ParsedForStatement? {
    val children = node.children()
    val forVariable = parseForVariable(children[1]) ?: return null
    val iterable = children[3]
    return ParsedForStatement(forVariable, iterable)
}

fun parseSliceExpression(node: SliceExpression): ParsedSliceExpression? {
    val children = node.children()
    if (children.size != 2)
        return null
    val slicesChildren = children[1].children()
    val slices = parseSlices(slicesChildren) ?: return null
    return ParsedSliceExpression(children[0], slices)
}

fun parseSlices(children: List<Node>): ParsedSlices? {
    if (children.any { it is Delimiter && it.toString() == "," }) {
        var i = 0
        val slices = mutableListOf<ParsedSlices>()
        while (i < children.size) {
            var j = children.drop(i).indexOfFirst { it is Delimiter && it.toString() == "," }
            if (j == -1)
                j = children.size
            val child = parseSlices(children.subList(i, j)) ?: return null
            slices.add(child)
            i = j
        }
        return TupleSlice(slices)
    }
    if (children.size != 3)
        return null
    if (children[1] is Delimiter && children[1].toString() == ":") {
        return SlicedSlice(null, null, null)
    }
    if (children[1] is Slice) {
        val sliceChildren = children[1].children()
        val i = sliceChildren.indexOfFirst { it is Delimiter && it.toString() == ":" }
        var j = sliceChildren.drop(i + 1).indexOfFirst { it is Delimiter && it.toString() == ":" }
        if (j == -1)
            j = sliceChildren.size
        val start = if (i == 0) null else if (i == 1) sliceChildren[0] else return null
        val end = if (j - i == 1) null else if (j - i == 2) sliceChildren[i + 1] else return null
        val step =
            if (j >= sliceChildren.size - 1) null else if (j == sliceChildren.size - 2) sliceChildren.last() else return null
        return SlicedSlice(start, end, step)
    }
    return SimpleSlice(children[1])
}

fun parseIfStatement(node: IfStatement): ParsedIfStatement =
    ParsedIfStatement(node.children().first { it !is Keyword })

fun parseConjunction(node: Conjunction): ParsedConjunction =
    ParsedConjunction(node.children()[0], node.children()[2])

fun parseDisjunction(node: Disjunction): ParsedDisjunction =
    ParsedDisjunction(node.children()[0], node.children()[2])

fun parseInversion(node: Inversion): ParsedInversion =
    ParsedInversion(node.children()[1])

fun parseGroup(node: Group): ParsedGroup =
    ParsedGroup(node.children().first { it !is Delimiter })

fun parseAdditiveExpression(node: AdditiveExpression): ParsedBinaryOperation? {  // TODO
    val op = (node.children()[1] as? Operator) ?: return null
    return ParsedBinaryOperation(node.children()[0], op, node.children()[2])
}

fun parseMultiplicativeExpression(node: MultiplicativeExpression): ParsedMultiplicativeExpression {
    val binaries: MutableList<ParsedBinaryOperation> = mutableListOf()
    val children = node.children()
    for (i in 0 until children.size - 2) {
        if (children[i + 1] !is Operator && children[i + 1] !is Delimiter)
            continue
        binaries.add(ParsedBinaryOperation(children[i], children[i + 1], children[i + 2]))
    }
    return ParsedMultiplicativeExpression(binaries)
}

fun parseList(node: org.parsers.python.ast.List): ParsedList {
    if (node.children().size <= 2)  // only delimiters
        return ParsedList(emptyList())
    val expr = node.children()[1]
    return if (expr is StarNamedExpressions)
        ParsedList(expr.children().filter { it !is Delimiter })
    else
        ParsedList(listOf(expr))
}

fun parseAssignment(node: Assignment): ParsedAssignment? {
    val op = node.children()[1] as? Delimiter ?: return null
    if (op.type == PythonConstants.TokenType.ASSIGN) {
        val targets = node.children().dropLast(1).filter { it !is Delimiter }
        return SimpleAssign(targets, node.children().last())
    }
    if (node.children().size != 3)
        return null
    return OpAssign(node.children()[0], op, node.children()[2])
}

fun parseDotName(node: DotName): ParsedDotName =
    ParsedDotName(node.children()[0], node.children()[2])

fun parseFunctionCall(node: FunctionCall): ParsedFunctionCall? {
    val function = node.children()[0]
    val args = (node.children()[1] as? InvocationArguments ?: return null).children().filter {
        if (it is Argument)  // for now ignore function calls with different argument kinds
            return null
        it !is Delimiter
    }
    return ParsedFunctionCall(function, args)
}

fun parseComparison(node: Comparison): ParsedComparison {
    val primitives: MutableList<PrimitiveComparison> = mutableListOf()
    val children = node.children()
    for (i in 0 until children.size - 2) {
        if (children[i + 1] !is Delimiter)
            continue
        primitives.add(PrimitiveComparison(children[i], children[i + 1] as Delimiter, children[i + 2]))
    }
    return ParsedComparison(primitives)
}