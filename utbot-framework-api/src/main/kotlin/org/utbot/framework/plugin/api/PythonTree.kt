package org.utbot.framework.plugin.api

object PythonTree {
    open class PythonTreeNode(
        val type: String,
    )

    class PrimitiveNode(
        type: String,
        val repr: String,
    ): PythonTreeNode(type)

    class ListNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode("builtins.list")

    class DictNode(
        val items: Map<PythonTreeNode, PythonTreeNode>
    ): PythonTreeNode("builtins.dict")

    class SetNode(
        val items: Set<PythonTreeNode>
    ): PythonTreeNode("builtins.set")

    class TupleNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode("builtins.tuple")

    class ReduceNode(
        type: String,
        val constructor: String,
        val args: List<PythonTreeNode>,
        val state: Map<String, PythonTreeNode>,
        val listitems: List<PythonTreeNode>,
        val dictitems: Map<PythonTreeNode, PythonTreeNode>,
    ): PythonTreeNode(type)
}
