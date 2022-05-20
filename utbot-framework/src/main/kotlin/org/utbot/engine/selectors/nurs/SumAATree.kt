package org.utbot.engine.selectors.nurs

import java.util.SortedSet
import java.util.Spliterator
import kotlin.math.min

/**
 * AATree is a strict variation of Red-Black tree.
 *
 *
 * Operations add/remove/contains/findLeftest has O(h) time complexity
 * where h is depth of the tree, that guaranteed no more than
 * 2 * logN.
 *
 * This assumption is guaranteed by invariant that all nodes
 * have parameter depth that:
 * * equals 1, if the node is a leaf
 * * equals parent.depth - 1 if it is the left child
 * * equals parent.depth or parent.depth - 1 if it is the
 *   right child.
 * * equals parent.parent.depth - 1 if it is the rightest
 *   grand child.
 *
 * All modifying operations on a tree are accompanied with
 * rebalancing operations skew, split and decreaseLevel.
 *
 * https://en.wikipedia.org/wiki/AA_tree
 *
 * @param Cmp - Type for comparison of elements
 * @property compareBy - function that transforms element to comparable type.
 * @property weight - function that transforms comparing element to weight
 *                    value for calculating the sum of weights in subtree.
 */
class SumAATree<T, Cmp : Comparable<Cmp>>(
    private val compareBy: (T) -> Cmp,
    private val weight: Cmp.() -> Double
) : AbstractSet<T>(), SortedSet<T> {
    private val elementsToWeights: MutableMap<T, Cmp> = mutableMapOf()

    private val comparator: Comparator<T> = compareBy { compareBy(it) }

    val sum: Double
        get() = root.sum

    override var size = 0

    private class Node<T, Cmp : Comparable<Cmp>>(
        val value: T,
        val comparing: Cmp,
        val level: Int,
        val left: Node<T, Cmp>?,
        val right: Node<T, Cmp>?,
        val sum: Double
    )

    private val Node<T, Cmp>?.sum: Double
        get() = this?.sum ?: 0.0

    /**
     * immutable update of this mode with given arguments
     */
    private fun Node<T, Cmp>.update(
        value: T = this.value,
        comparing: Cmp = this.comparing,
        level: Int = this.level,
        left: Node<T, Cmp>? = this.left,
        right: Node<T, Cmp>? = this.right
    ) = Node(
        value, comparing, level, left, right,
        left.sum + right.sum + comparing.weight()
    )

    private var root: Node<T, Cmp>? = null

    /**
     * Get rid of case where left child has the same depth.
     *
     *    L <-- T              L --> T
     *   / \     \    ----->  /     / \
     *  A   B     R          A     B   R
     */
    private fun Node<T, Cmp>.skew(): Node<T, Cmp> = when {
        this.left == null -> this
        this.left.level == this.level -> {
            this.left.update(right = this.update(left = this.left.right))
        }
        else -> this
    }

    /**
     * Get rid of case where there is a rightest grandchild
     * with the same depth.
     *
     *    T --> R --> X            R
     *   /     /         ----->   / \
     *  A     B                  T   X
     *                          / \
     *                         A   B
     */
    private fun Node<T, Cmp>.split(): Node<T, Cmp> = when {
        this.right?.right == null -> this
        this.level == this.right.right.level -> {
            this.right.update(
                level = this.right.level + 1,
                left = this.update(right = this.right.left)
            )
        }
        else -> this
    }

    private val Node<T, Cmp>?.level
        get() = this?.level ?: 0

    /**
     * The leftest node in the subtree relating to this node.
     */
    private val Node<T, Cmp>.succ: Node<T, Cmp>?
        get() {
            var node = this.right
            while (node?.left != null) {
                node = node.left
            }
            return node
        }

    /**
     * The rightest node in the subtree relating to this node.
     */
    private val Node<T, Cmp>.pred: Node<T, Cmp>?
        get() {
            var node = this.left
            while (node?.right != null) {
                node = node.right
            }
            return node
        }

    /**
     * Recalculate level to not violate the invariant
     */
    private fun Node<T, Cmp>.decreaseLevel(): Node<T, Cmp> {
        val shouldBe = min(this.left.level, this.right.level) + 1
        return if (shouldBe < this.level) {
            val right = if (shouldBe < this.right.level) {
                this.right!!.update(level = shouldBe)
            } else {
                this.right
            }
            this.update(level = shouldBe, right = right)
        } else {
            this
        }
    }


    /**
     * find an element by its comparing value in the subtree.
     */
    private fun find(comparing: Cmp?, node: Node<T, Cmp>?): Node<T, Cmp>? = when {
        node == null -> null
        comparing == null -> null
        comparing < node.comparing -> find(comparing, node.left)
        comparing > node.comparing -> find(comparing, node.right)
        else -> node
    }

    /**
     * Delete an element with given comparing value from the subtree.
     */
    private fun delete(comparing: Cmp, node: Node<T, Cmp>?): Node<T, Cmp>? {
        val tmp: Node<T, Cmp>? = when {
            node == null -> null
            comparing > node.comparing -> {
                node.update(right = delete(comparing, node.right))
            }
            comparing < node.comparing -> {
                node.update(left = delete(comparing, node.left))
            }
            else -> {
                when {
                    node.right == null && node.level == 1 -> {
                        size--
                        null
                    }
                    node.left == null -> {
                        val l = node.succ!! // right exists
                        node.update(
                            value = l.value,
                            comparing = l.comparing,
                            right = delete(l.comparing, node.right)
                        )
                    }
                    else -> {
                        val l = node.pred!! // left exists
                        node.update(
                            value = l.value,
                            comparing = l.comparing,
                            left = delete(l.comparing, node.left)
                        )
                    }
                }
            }
        }?.decreaseLevel()?.skew()
        return if (tmp == null) {
            null
        } else {
            val right = tmp.right?.skew()
            val rightright = right?.right?.skew()

            val res = tmp.update(
                right = right
                    ?.update(right = rightright)
            ).split()

            res.update(right = res.right?.split())
        }
    }

    /**
     * insert in the subtree new element with given comparing value
     */
    private fun insert(element: T, comparing: Cmp, node: Node<T, Cmp>?): Node<T, Cmp> {
        val res: Node<T, Cmp> = when {
            node == null -> {
                size += 1
                Node(element, comparing, 1, null, null, comparing.weight())
            }
            comparing < node.comparing -> {
                node.update(left = insert(element, comparing, node.left))
            }
            comparing > node.comparing -> {
                node.update(right = insert(element, comparing, node.right))
            }
            else -> node
        }
        return res.skew().split()
    }


    private fun add(element: T, comparing: Cmp): Boolean {
        val prevSize = size
        root = insert(element, comparing, root)
        return if (size != prevSize) {
            elementsToWeights[element] = comparing
            true
        } else {
            false
        }
    }

    override fun add(element: T): Boolean =
        add(element, compareBy(element))


    override fun contains(element: T): Boolean =
        elementsToWeights.contains(element)


    private fun remove(element: T, comparing: Cmp): Boolean {
        val prevSize = size
        root = delete(comparing, root)
        return if (size != prevSize) {
            elementsToWeights.remove(element)
            true
        } else {
            false
        }
    }

    override fun remove(element: T): Boolean {
        val comparing = elementsToWeights[element] ?: throw NoSuchElementException()
        return remove(element, comparing)
    }


    private fun update(element: T, newCmp: Cmp): Boolean {
        val previousCmp = elementsToWeights[element] ?: throw NoSuchElementException()
        if (previousCmp == newCmp) {
            return true
        }
        val node = find(previousCmp, root) ?: throw NoSuchElementException()
        val prevSize = size
        val prevRoot = root
        root = delete(previousCmp, root)
        return if (size == prevSize - 1) {
            root = insert(node.value, newCmp, root)
            if (size == prevSize) {
                elementsToWeights[node.value] = newCmp
                true
            } else {
                root = prevRoot
                false
            }
        } else {
            false
        }
    }

    /**
     * Recalculate comparing value of the element and
     * update its position in the tree according to new value.
     *
     * @return true if element has changed.
     */
    fun update(element: T): Boolean = update(element, compareBy(element))

    /**
     * Updates all the elements in the tree
     *
     * @see update
     */
    fun updateAll(): Boolean = elementsToWeights.keys
        .fold(false) { changed, element ->
            update(element) || changed
        }

    private fun Node<T, Cmp>.findLeftest(givenSum: Double, initSum: Double = 0.0): Pair<T, Double> {
        val accLeft = left.sum + initSum
        val accLeftThis = accLeft + comparing.weight()
        val res = when {
            accLeft > givenSum -> left?.findLeftest(givenSum, initSum)
            accLeftThis > givenSum -> value to accLeftThis
            else -> right?.findLeftest(givenSum, accLeftThis)
        }
        return res ?: value to accLeftThis
    }

    /**
     * Find such the lowest element in the tree that sum of all
     * the lower elements is greater or equal to given sum
     */
    fun findLeftest(sum: Double): Pair<T, Double>? = root?.findLeftest(sum)


    override fun comparator(): Comparator<in T> = comparator


    override fun first(): T {
        var node = root
        while (node?.left != null) {
            node = node.left
        }
        return node?.value ?: throw NoSuchElementException()
    }

    override fun last(): T {
        var node = root
        while (node?.right != null) {
            node = node.right
        }
        return node?.value ?: throw NoSuchElementException()
    }

    override fun addAll(elements: Collection<T>): Boolean = elements
        .fold(false) { changed, element ->
            add(element) || changed
        }

    override fun clear() {
        root = null
        size = 0
    }

    override fun removeAll(elements: Collection<T>): Boolean = elements
        .fold(false) { changed, element ->
            remove(element) || changed
        }


    /**
     * AATree iterator that traverses the tree in O(n) time complexity from
     * minimal to maximal element.
     */
    private inner class SumAATreeIterator : MutableIterator<T> {
        private val parentQueue = mutableListOf<Node<T, Cmp>>()
        private var next: Node<T, Cmp>? = root
        private var cur: Node<T, Cmp>? = null

        init {
            while (next?.left != null) {
                parentQueue += next!!
                next = next!!.left
            }
        }

        override fun hasNext(): Boolean {
            return parentQueue.isNotEmpty() || next != null
        }

        override fun next(): T {
            val cur = next
            if (next?.right == null && parentQueue.isNotEmpty()) {
                next = parentQueue.removeLast()
            } else {
                next = next?.right
                while (next?.left != null) {
                    parentQueue += next!!
                    next = next?.left
                }
            }
            return cur!!.value
        }

        override fun remove() {
            if (cur != null) {
                root = delete(cur!!.comparing, root)
                cur = null
            }
            next = find(next?.comparing, root)
        }
    }

    override fun iterator(): MutableIterator<T> {
        return SumAATreeIterator()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val iterator = iterator()
        var changed = false
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element !in elements) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }

    override fun spliterator(): Spliterator<T> {
        return super<SortedSet>.spliterator()
    }

    override fun subSet(fromElement: T, toElement: T): SortedSet<T> {
        throw NotImplementedError()
    }

    override fun headSet(toElement: T): SortedSet<T> {
        throw NotImplementedError()
    }

    override fun tailSet(fromElement: T): SortedSet<T> {
        throw NotImplementedError()
    }
}
