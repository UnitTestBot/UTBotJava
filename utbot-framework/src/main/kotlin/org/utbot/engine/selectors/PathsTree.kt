package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import java.util.NoSuchElementException
import kotlin.random.Random

/**
 * Paths tree where nodes can contain executionState.
 * add/remove/find/choose works with time O(h) where h -
 * is number of forks on the path.
 *
 * @see RandomPathSelector
 */
class PathsTree(private val random: Random) {
    private val root = DummyNode()

    /**
     * Add new node to the tree
     */
    operator fun plusAssign(state: ExecutionState) =
        root.add(state)

    /**
     * Choose a state with random path in the tree
     */
    fun choose(): ExecutionState? =
        root.choose(random)?.state

    /**
     * remove state from the tree or throw NoSuchElementException()
     */
    fun remove(state: ExecutionState): Boolean = root.remove(state) != null

    val size
        get() = root.size


    fun forEach(block: (ExecutionState) -> Unit) {
        val stack = mutableListOf<TreeNode>(root)
        while (stack.isNotEmpty()) {
            val v = stack.removeLast()
            if (v is StatedNode) {
                block(v.state)
            } else {
                stack += v.children
            }
        }
    }

    private abstract class TreeNode(val depth: Int, var size: Int = 0) {
        var children = mutableListOf<Node>()

        private fun next(decisionPath: List<Int>): Node? =
            children.firstOrNull {
                it.depth <= decisionPath.size && decisionPath[it.depth] == it.decisionNum
            }


        open fun choose(random: Random): StatedNode? {
            if (children.size == 0) {
                return null
            }
            val ind = random.nextInt(children.size)
            return children[ind].choose(random)
        }

        private fun nextIndexed(decisionPath: List<Int>): Int =
            children.indexOfFirst {
                it.depth <= decisionPath.size && decisionPath[it.depth] == it.decisionNum
            }

        open fun remove(state: ExecutionState): TreeNode? {
            val ind = nextIndexed(state.decisionPath)
            if (ind == -1) {
                throw NoSuchElementException()
            } else {
                size -= children[ind].size
                val node = children[ind].remove(state)
                if (node == null) {
                    children.removeAt(ind)
                } else {
                    size += node.size
                    children[ind] = node as Node
                }
            }
            return if (children.isEmpty()) {
                // return null if should return
                null
            } else {
                this
            }
        }

        fun add(state: ExecutionState) {
            val ind = nextIndexed(state.decisionPath)

            if (ind == -1) {
                children.add(make(state))

                size++
            } else {
                size -= children[ind].size
                val node = children[ind].apply { add(state) }
                size += node.size

                if (children.size == 1 && node !is StatedNode && node.children.size == 1) {

                    children[0] = node.children[0]
                }
            }
        }

        open fun find(state: ExecutionState): StatedNode? =
            next(state.decisionPath)?.find(state)

        private fun make(state: ExecutionState): Node {
            return if (depth < state.pathLength - 1) {
                Node(state.decisionPath[depth + 1], depth + 1).apply {
                    add(state)
                }
            } else {
                StatedNode(state.decisionPath.last(), depth + 1, state)
            }
        }
    }

    private class DummyNode : TreeNode(-1)

    private open class Node(val decisionNum: Int, depth: Int) : TreeNode(depth)

    private class StatedNode(decisionNum: Int, depth: Int, val state: ExecutionState) : Node(decisionNum, depth) {

        init {
            size = 1
        }

        override fun find(state: ExecutionState): StatedNode? {
            return if (state.pathLength == depth) {
                this
            } else {
                super.find(state)
            }
        }

        override fun remove(state: ExecutionState): TreeNode? {
            return if (state == state) {
                return if (children.isEmpty()) {
                    null
                } else {
                    // remove statement, but node stays
                    Node(state.decisionPath.last(), depth).apply { children.addAll(this@StatedNode.children) }
                }
            } else {
                // don't delete this node as it still holds ExecutionState
                super.remove(state) ?: this
            }
        }

        override fun choose(random: Random): StatedNode? {
            val ind = random.nextInt(children.size + 1)
            return if (ind == children.size) {
                this
            } else {
                children[ind].choose(random)
            }
        }
    }
}