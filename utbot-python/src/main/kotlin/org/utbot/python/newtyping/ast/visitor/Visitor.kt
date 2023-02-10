package org.utbot.python.newtyping.ast.visitor

import org.parsers.python.Node

class Visitor(private val collectors: List<Collector>) {
    fun visit(node: Node) {
        fixOffsets(node)  // bug in parser?
        innerVisit(node)
        collectors.forEach {
            it.finishCollection()
        }
    }

    private fun fixOffsets(node: Node) {
        node.children().forEach { fixOffsets(it) }
        if (node.children().isNotEmpty()) {
            node.beginOffset = node.children().first().beginOffset
            node.endOffset = node.children().last().endOffset
        }
    }

    private fun innerVisit(node: Node) {
        collectors.forEach { it.collectFromNodeBeforeRecursion(node) }
        node.children().forEach {
            innerVisit(it)
        }
        collectors.forEach { it.collectFromNodeAfterRecursion(node) }
    }
}