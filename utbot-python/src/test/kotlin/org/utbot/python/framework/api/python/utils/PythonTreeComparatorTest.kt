package org.utbot.python.framework.api.python.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.comparePythonTree
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId

internal class PythonTreeComparatorTest {
    @Test
    fun testEqualPrimitive() {
        val left = PythonTree.PrimitiveNode(pythonIntClassId, "1")

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualList() {
        val left = PythonTree.ListNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualTuple() {
        val left = PythonTree.TupleNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualDict() {
        val left = PythonTree.DictNode(mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'a'") to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualSet() {
        val left = PythonTree.SetNode(setOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableSet())

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualEmptyReduce() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(PythonTree.PrimitiveNode(pythonIntClassId, "2")),
        )

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualNotEmptyReduce() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(PythonTree.PrimitiveNode(pythonIntClassId, "2")),
        )
        left.state["my_field"] = PythonTree.PrimitiveNode(pythonIntClassId, "2")
        left.state["my_field_1"] = PythonTree.PrimitiveNode(pythonIntClassId, "1")

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualReduceWithListItems() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(PythonTree.PrimitiveNode(pythonIntClassId, "2")),
        )
        left.listitems = listOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        )

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualReduceWithDictItems() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(PythonTree.PrimitiveNode(pythonIntClassId, "2")),
        )
        left.dictitems = mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'a'") to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        )

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualRecursiveReduce() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child2 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        left.state["children"] = PythonTree.ListNode(
            mapOf(0 to child).toMutableMap()
        )
        child.state["children"] = PythonTree.ListNode(
            mapOf(0 to child2).toMutableMap()
        )
        child2.state["children"] = PythonTree.ListNode(
            mapOf(0 to left).toMutableMap()
        )

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testEqualHardRecursiveReduce() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child2 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child3 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child4 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child5 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val child6 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        left.state["children"] = PythonTree.ListNode(mapOf(
            0 to child,
            1 to child2,
            2 to child3,
        ).toMutableMap())
        child.state["children"] = PythonTree.ListNode(mapOf(
            0 to child2,
            1 to child3,
        ).toMutableMap())
        child2.state["children"] = PythonTree.ListNode(mapOf(
            0 to child2,
            1 to child3,
            2 to left,
            3 to child4,
        ).toMutableMap())
        child3.state["children"] = PythonTree.ListNode(mapOf(
            0 to left,
        ).toMutableMap())
        child4.state["children"] = PythonTree.ListNode(mapOf(
            0 to left,
            1 to child5,
        ).toMutableMap())
        child5.state["children"] = PythonTree.ListNode(mapOf(
            0 to child2,
        ).toMutableMap())
        child6.state["children"] = PythonTree.ListNode(mapOf(
            0 to child2,
        ).toMutableMap())

        assertTrue(comparePythonTree(left, left))
    }

    @Test
    fun testNotEqualPrimitive() {
        val left = PythonTree.PrimitiveNode(pythonIntClassId, "1")
        val right = PythonTree.PrimitiveNode(pythonIntClassId, "2")

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualList() {
        val left = PythonTree.ListNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())
        val right = PythonTree.ListNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "0"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualTuple() {
        val left = PythonTree.TupleNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())
        val right = PythonTree.TupleNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "0"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualDict() {
        val left = PythonTree.DictNode(mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'c'") to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "0"),
        ).toMutableMap())
        val right = PythonTree.DictNode(mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'a'") to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualSet() {
        val left = PythonTree.SetNode(setOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableSet())
        val right = PythonTree.SetNode(setOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "0"),
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableSet())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualListDiffSize() {
        val left = PythonTree.ListNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())
        val right = PythonTree.ListNode(mapOf(
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualTupleDiffSize() {
        val left = PythonTree.TupleNode(mapOf(
            0 to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())
        val right = PythonTree.TupleNode(mapOf(
            1 to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualDictDiffSize() {
        val left = PythonTree.DictNode(mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'c'") to PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())
        val right = PythonTree.DictNode(mapOf(
            PythonTree.PrimitiveNode(pythonStrClassId, "'b'") to PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualSetDiffSize() {
        val left = PythonTree.SetNode(setOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "1"),
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableSet())
        val right = PythonTree.SetNode(setOf(
            PythonTree.PrimitiveNode(pythonIntClassId, "2"),
        ).toMutableSet())

        assertFalse(comparePythonTree(left, right))
    }

    @Test
    fun testNotEqualRecursiveReduce() {
        val left = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val leftChild1 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val leftChild2 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        left.state["children"] = PythonTree.ListNode(
            mapOf(0 to leftChild1).toMutableMap()
        )
        leftChild1.state["children"] = PythonTree.ListNode(
            mapOf(0 to leftChild2).toMutableMap()
        )
        leftChild2.state["children"] = PythonTree.ListNode(
            mapOf(0 to left).toMutableMap()
        )

        val right = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val rightChild1 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        val rightChild2 = PythonTree.ReduceNode(
            PythonClassId("my_module", "MyClass"),
            PythonClassId("my_module.MyClass"),
            listOf(),
        )
        right.state["children"] = PythonTree.ListNode(mapOf(
            0 to rightChild1,
            1 to rightChild2,
        ).toMutableMap())
        rightChild1.state["children"] = PythonTree.ListNode(mapOf(
            0 to rightChild2,
        ).toMutableMap())

        assertFalse(comparePythonTree(left, right))
    }
}