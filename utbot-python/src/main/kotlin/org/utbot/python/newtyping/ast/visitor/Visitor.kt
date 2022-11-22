package org.utbot.python.newtyping.ast.visitor

import org.parsers.python.Node

class Visitor(private val collectors: List<Collector>) {
    fun visit(node: Node) {
        innerVisit(node)
        collectors.forEach {
            it.finishCollection()
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