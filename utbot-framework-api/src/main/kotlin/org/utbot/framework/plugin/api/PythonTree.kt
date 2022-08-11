package org.utbot.framework.plugin.api

object PythonTree {
    open class PythonTreeNode(
        val type: PythonClassId,
    )

    class PrimitiveNode(
        type: PythonClassId,
        val repr: String,
    ): PythonTreeNode(type)

    class ListNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.list"))

    class DictNode(
        val items: Map<PythonTreeNode, PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.dict"))

    class SetNode(
        val items: Set<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.set"))

    class TupleNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.tuple"))

    class ReduceNode(
        type: PythonClassId,
        val constructor: String,
        val args: List<PythonTreeNode>,
        val state: Map<String, PythonTreeNode>,
        val listitems: List<PythonTreeNode>,
        val dictitems: Map<PythonTreeNode, PythonTreeNode>,
    ): PythonTreeNode(type)
}
