package org.utbot.framework.plugin.api

object PythonTree {
    open class PythonTreeNode(
        val type: PythonClassId,
    ) {
        open val children: List<PythonTreeNode> = emptyList()
    }

    class PrimitiveNode(
        type: PythonClassId,
        val repr: String,
    ): PythonTreeNode(type)

    class ListNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.list")) {
        override val children: List<PythonTreeNode>
            get() = items
    }

    class DictNode(
        val items: Map<PythonTreeNode, PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.dict")) {
        override val children: List<PythonTreeNode>
            get() = items.values + items.keys
    }

    class SetNode(
        val items: Set<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.set")) {
        override val children: List<PythonTreeNode>
            get() = items.toList()
    }

    class TupleNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.tuple")) {
        override val children: List<PythonTreeNode>
            get() = items
    }

    class ReduceNode(
        type: PythonClassId,
        val constructor: String,
        val args: List<PythonTreeNode>,
        val state: Map<String, PythonTreeNode>,
        val listitems: List<PythonTreeNode>,
        val dictitems: Map<PythonTreeNode, PythonTreeNode>,
    ): PythonTreeNode(type)  {
        override val children: List<PythonTreeNode>
            get() = args + state.values + listitems + dictitems.values + dictitems.keys
    }
}
