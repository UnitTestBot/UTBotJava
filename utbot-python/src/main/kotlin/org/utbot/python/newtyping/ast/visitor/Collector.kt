package org.utbot.python.newtyping.ast.visitor

import org.parsers.python.Node

abstract class Collector {
    open fun collectFromNodeBeforeRecursion(node: Node) = run { }
    open fun collectFromNodeAfterRecursion(node: Node) = run {  }
    open fun finishCollection() = run { }
}