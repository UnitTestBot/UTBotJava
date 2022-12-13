package org.utbot.python.newtyping.ast

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.*

data class ParsedFunctionDefinition(val name: Name, val body: Block)
data class ParsedForStatement(val forVariable: Name, val iterable: Node)
sealed class ParsedSlices
data class SimpleSlice(val indexedValue: Node): ParsedSlices()
data class ComplexSlice(val indexValues: List<Node>): ParsedSlices()
data class ParsedIfStatement(val condition: Node)
data class ParsedConjunction(val left: Node, val right: Node)
data class ParsedDisjunction(val left: Node, val right: Node)
data class ParsedInversion(val expr: Node)
data class ParsedGroup(val expr: Node)
data class ParsedList(val elems: List<Node>)
sealed class ParsedAssignment
data class SimpleAssign(val targets: List<Node>, val value: Node): ParsedAssignment()
data class OpAssign(val target: Node, val op: Delimiter, val value: Node): ParsedAssignment()
data class ParsedBinaryOperation(val left: Node, val op: Delimiter, val right: Node)
data class ParsedDotName(val head: Node, val tail: Node)

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

fun parseForStatement(node: ForStatement): ParsedForStatement {
    val forVariableIndex = node.children().indexOfFirst { it is Name }
    val iterable = node.children().drop(forVariableIndex + 1).first { it !is Keyword }
    return ParsedForStatement(node.children()[forVariableIndex] as Name, iterable)
}

fun parseSlices(node: Slices): ParsedSlices {
    val child = node.children()[1]
    if (child is Slice) {
        return ComplexSlice(
            child.children().filter { it !is Delimiter }
        )
    }
    return SimpleSlice(child)
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

fun parseAdditiveExpression(node: AdditiveExpression): ParsedBinaryOperation? {
    val op = (node.children()[1] as? Delimiter) ?: return null
    return ParsedBinaryOperation(node.children()[0], op, node.children()[2])
}

fun parseMultiplicativeExpression(node: MultiplicativeExpression): ParsedBinaryOperation? {
    val op = (node.children()[1] as? Delimiter) ?: return null
    return ParsedBinaryOperation(node.children()[0], op, node.children()[2])
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