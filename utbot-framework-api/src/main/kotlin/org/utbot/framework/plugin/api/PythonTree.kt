package org.utbot.framework.plugin.api

typealias PythonId = Long

object PythonTree {
    open class PythonTreeNode(
        val type: PythonClassId,
        var comparable: Boolean = true
    ) {
        open val children: List<PythonTreeNode> = emptyList()

        open fun typeEquals(other: Any?): Boolean {
            return if (other is PythonTreeNode)
                type == other.type
            else
                false
        }
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

        override fun typeEquals(other: Any?): Boolean {
            return if (other is ListNode)
                items.zip(other.items).all {
                    it.first.typeEquals(it.second)
                }
            else false
        }
    }

    class DictNode(
        val items: Map<PythonTreeNode, PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.dict")) {
        override val children: List<PythonTreeNode>
            get() = items.values + items.keys

        override fun typeEquals(other: Any?): Boolean {
            return if (other is DictNode) {
                items.keys.size == other.items.keys.size && items.keys.all {
                    items[it]?.typeEquals(other.items[it]) ?: false
                }

            } else false
        }
    }

    class SetNode(
        val items: Set<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.set")) {
        override val children: List<PythonTreeNode>
            get() = items.toList()

        override fun typeEquals(other: Any?): Boolean {
            return if (other is SetNode) {
                items.size == other.items.size && (
                        items.isEmpty() || items.all {
                            items.first().typeEquals(it)
                        } && other.items.all {
                            items.first().typeEquals(it)
                        })
            } else {
                false
            }
        }
    }

    class TupleNode(
        val items: List<PythonTreeNode>
    ): PythonTreeNode(PythonClassId("builtins.tuple")) {
        override val children: List<PythonTreeNode>
            get() = items

        override fun typeEquals(other: Any?): Boolean {
            return if (other is TupleNode) {
                items.size == other.items.size && items.zip(other.items).all {
                    it.first.typeEquals(it.second)
                }
            } else {
                false
            }
        }
    }

    class ReduceNode(
        val id: PythonId,
        type: PythonClassId,
        val constructor: String,
        val args: List<PythonTreeNode>,
        var state: Map<String, PythonTreeNode>,
        var listitems: List<PythonTreeNode>,
        var dictitems: Map<PythonTreeNode, PythonTreeNode>,
    ): PythonTreeNode(type) {
        constructor(
            id: PythonId,
            type: PythonClassId,
            constructor: String,
            args: List<PythonTreeNode>,
        ): this(id, type, constructor, args, emptyMap(), emptyList(), emptyMap())

        override val children: List<PythonTreeNode>
            get() = args + state.values + listitems + dictitems.values + dictitems.keys

        override fun typeEquals(other: Any?): Boolean {
            return if (other is ReduceNode)
                type == other.type
            else false
        }
    }

    fun allElementsHaveSameStructure(elements: Collection<PythonTreeNode>): Boolean {
        return if (elements.isEmpty()) {
            true
        } else {
            val firstElement = elements.first()
            elements.drop(1).all {
                it.typeEquals(firstElement)
            }
        }
    }
}
