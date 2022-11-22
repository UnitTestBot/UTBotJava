package org.utbot.python.newtyping.ast

import org.parsers.python.Node
import org.parsers.python.ast.*

fun isIdentification(node: Node): Boolean {
    val name = node as? Name ?: return false
    val parent = name.parent as? DotName ?: return true
    return parent.children().first() == node
}

data class ParsedForStatement(
    val forVariable: Name,
    val iterable: Node
)

fun parseForStatement(node: ForStatement): ParsedForStatement {
    val forVariableIndex = node.children().indexOfFirst { it is Name }
    val iterable = node.children().drop(forVariableIndex + 1).first { it !is Keyword }
    return ParsedForStatement(node.children()[forVariableIndex] as Name, iterable)
}

sealed class ParsedSlices

data class SimpleSlice(
    val indexedValue: Node
): ParsedSlices()

data class ComplexSlice(
    val indexValues: List<Node>
): ParsedSlices()

fun parseSlices(node: Slices): ParsedSlices {
    val child = node.children()[1]
    if (child is Slice) {
        return ComplexSlice(
            child.children().filter { it !is Delimiter }
        )
    }
    return SimpleSlice(child)
}