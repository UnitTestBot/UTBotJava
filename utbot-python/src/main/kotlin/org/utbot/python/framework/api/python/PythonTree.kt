package org.utbot.python.framework.api.python

import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId

object PythonTree {
    open class PythonTreeNode(
        val type: PythonClassId,
        var comparable: Boolean = true
    ) {
        open val children: List<PythonTreeNode> = emptyList()

        open fun typeEquals(other: Any?): Boolean {
            return if (other is PythonTreeNode)
                type == other.type && comparable && other.comparable
            else
                false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PythonTreeNode) {
                return false
            }
            return type == other.type && children == other.children
        }
    }

    class PrimitiveNode(
        type: PythonClassId,
        val repr: String,
    ) : PythonTreeNode(type) {
        override fun equals(other: Any?): Boolean {
            if (other !is PrimitiveNode) {
                return false
            }
            return repr == other.repr && type == other.type
        }
    }

    class ListNode(
        val items: MutableMap<Int, PythonTreeNode>
    ) : PythonTreeNode(PythonClassId("builtins.list")) {
        override val children: List<PythonTreeNode>
            get() = items.values.toList()

        override fun typeEquals(other: Any?): Boolean {
            return if (other is ListNode)
                children.zip(other.children).all {
                    it.first.typeEquals(it.second)
                }
            else false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is ListNode) {
                return false
            }
            return type == other.type && children == other.children
        }
    }

    class DictNode(
        val items: MutableMap<PythonTreeNode, PythonTreeNode>
    ) : PythonTreeNode(PythonClassId("builtins.dict")) {
        override val children: List<PythonTreeNode>
            get() = items.values + items.keys

        override fun typeEquals(other: Any?): Boolean {
            return if (other is DictNode) {
                items.keys.size == other.items.keys.size && items.keys.all {
                    items[it]?.typeEquals(other.items[it]) ?: false
                }

            } else false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is DictNode) {
                return false
            }
            return type == other.type && children == other.children
        }
    }

    class SetNode(
        val items: MutableSet<PythonTreeNode>
    ) : PythonTreeNode(PythonClassId("builtins.set")) {
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

        override fun equals(other: Any?): Boolean {
            if (other !is SetNode) {
                return false
            }
            return type == other.type && children == other.children
        }
    }

    class TupleNode(
        val items: MutableMap<Int, PythonTreeNode>
    ) : PythonTreeNode(PythonClassId("builtins.tuple")) {
        override val children: List<PythonTreeNode>
            get() = items.values.toList()

        override fun typeEquals(other: Any?): Boolean {
            return if (other is TupleNode) {
                items.size == other.items.size && children.zip(other.children).all {
                    it.first.typeEquals(it.second)
                }
            } else {
                false
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other !is TupleNode) {
                return false
            }
            return type == other.type && children == other.children
        }
    }

    class ReduceNode(
        val id: Long,
        type: PythonClassId,
        val constructor: PythonClassId,
        val args: List<PythonTreeNode>,
        var state: MutableMap<String, PythonTreeNode>,
        var listitems: List<PythonTreeNode>,
        var dictitems: Map<PythonTreeNode, PythonTreeNode>,
    ) : PythonTreeNode(type) {
        constructor(
            id: Long,
            type: PythonClassId,
            constructor: PythonClassId,
            args: List<PythonTreeNode>,
        ) : this(id, type, constructor, args, emptyMap<String, PythonTreeNode>().toMutableMap(), emptyList(), emptyMap())

        override val children: List<PythonTreeNode>
            get() = args + state.values + listitems + dictitems.values + dictitems.keys + PythonTreeNode(constructor)

        override fun typeEquals(other: Any?): Boolean {
            return if (other is ReduceNode) {
                type == other.type && state.all { (key, value) ->
                    other.state.containsKey(key) && value.typeEquals(other.state[key])
                } && listitems.withIndex().all { (index, item) ->
                    other.listitems.size > index && item.typeEquals(other.listitems[index])
                } && dictitems.all { (key, value) ->
                    other.dictitems.containsKey(key) && value.typeEquals(other.dictitems[key])
                }
            } else false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is ReduceNode) {
                return false
            }
            return type == other.type && children == other.children
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

    fun fromObject(): PrimitiveNode {
        return PrimitiveNode(
            PythonClassId("builtins.object"),
            "object"
        )
    }

    fun fromNone(): PrimitiveNode {
        return PrimitiveNode(
            pythonNoneClassId,
            "None"
        )
    }

    fun fromInt(value: Int): PrimitiveNode {
        return PrimitiveNode(
            pythonIntClassId,
            value.toString()
        )
    }

    fun fromString(value: String): PrimitiveNode {
        return PrimitiveNode(
            pythonIntClassId,
            value
        )
    }

    fun fromBool(value: Boolean): PrimitiveNode {
        return PrimitiveNode(
            pythonBoolClassId,
            if (value) "True" else "False"
        )
    }

    fun fromFloat(value: Double): PrimitiveNode {
        return PrimitiveNode(
            pythonFloatClassId,
            value.toString()
        )
    }
}