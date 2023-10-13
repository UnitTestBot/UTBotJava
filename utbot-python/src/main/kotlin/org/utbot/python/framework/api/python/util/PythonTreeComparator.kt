package org.utbot.python.framework.api.python.util

import org.utbot.python.framework.api.python.PythonTree

enum class VisitStatus {
    OPENED, CLOSED
}

/*
 * Compare python tree by structure. Returns false if:
 *  - objects have different types
 *  - incomparable and have different ids
 *  - have the same type but structures aren't equal recursively
 */
fun comparePythonTree(
    left: PythonTree.PythonTreeNode,
    right: PythonTree.PythonTreeNode,
    visitedLeft: MutableMap<Long, VisitStatus> = emptyMap<Long, VisitStatus>().toMutableMap(),
    visitedRight: MutableMap<Long, VisitStatus> = emptyMap<Long, VisitStatus>().toMutableMap(),
    equals: MutableMap<Pair<Long, Long>, Boolean> = emptyMap<Pair<Long, Long>, Boolean>().toMutableMap(),
    ): Boolean {
    if (visitedLeft[left.id] != visitedRight[right.id]) {
        visitedLeft[left.id] = VisitStatus.CLOSED
        visitedRight[right.id] = VisitStatus.CLOSED
        equals[left.id to right.id] = false
        return false
    }
    if (visitedLeft[left.id] == VisitStatus.CLOSED) {
        return equals[left.id to right.id] ?: false
    }
    if (visitedLeft[left.id] == VisitStatus.OPENED) {
        return true
    }

    visitedLeft[left.id] = VisitStatus.OPENED
    visitedRight[right.id] = VisitStatus.OPENED

    val areEquals = if (left.comparable && right.comparable && left.type == right.type) {
        when (left) {
            is PythonTree.PrimitiveNode -> {
                left == right
            }

            is PythonTree.ListNode -> {
                if (right !is PythonTree.ListNode) false
                else if (left.items.keys != right.items.keys) false
                else left.items.keys.all { comparePythonTree(left.items[it]!!, right.items[it]!!, visitedLeft, visitedRight, equals) }
            }

            is PythonTree.DictNode -> {
                if (right !is PythonTree.DictNode) false
                else if (left.items.keys != right.items.keys) false
                else left.items.keys.all { comparePythonTree(left.items[it]!!, right.items[it]!!, visitedLeft, visitedRight, equals) }
            }

            is PythonTree.TupleNode -> {
                if (right !is PythonTree.TupleNode) false
                else if (left.items.keys != right.items.keys) false
                else left.items.keys.all { comparePythonTree(left.items[it]!!, right.items[it]!!, visitedLeft, visitedRight, equals) }
            }

            is PythonTree.SetNode -> {
                if (right !is PythonTree.SetNode) false
                else if (left.items.size != right.items.size) false
                else left.items.sortedBy { it.id }
                    .zip(right.items.sortedBy { it.id })
                    .all { comparePythonTree(it.first, it.second, visitedLeft, visitedRight, equals) }
            }

            is PythonTree.ReduceNode -> {
                if (right !is PythonTree.ReduceNode) false
                else {
                    val type = left.type == right.type
                    val state = left.state.size == right.state.size && left.state.keys.all {
                        comparePythonTree(
                            left.state[it]!!,
                            right.state[it]!!,
                            visitedLeft,
                            visitedRight,
                            equals
                        )
                    }
                    val listitems = left.listitems.size == right.listitems.size && left.listitems.zip(right.listitems)
                        .all { comparePythonTree(it.first, it.second, visitedLeft, visitedRight, equals) }
                    val dictitems = left.dictitems.keys == right.dictitems.keys && left.dictitems.keys
                        .all { comparePythonTree(left.dictitems[it]!!, right.dictitems[it]!!, visitedLeft, visitedRight, equals) }

                    type && state && listitems && dictitems
                }
            }

            else -> false
        }
    } else left.id == right.id

    visitedLeft[left.id] = VisitStatus.CLOSED
    visitedRight[right.id] = VisitStatus.CLOSED
    equals[left.id to right.id] = areEquals
    return areEquals
}