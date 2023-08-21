package org.utbot.fuzzing.utils

fun <T> trieOf(vararg values: Iterable<T>): Trie<T, T> = IdentityTrie<T>().apply {
    values.forEach(this::add)
}

fun stringTrieOf(vararg values: String): StringTrie = StringTrie().apply {
    values.forEach(this::add)
}

class StringTrie : IdentityTrie<Char>() {
    fun add(string: String) = super.add(string.toCharArray().asIterable())
    fun removeCompletely(string: String) = super.removeCompletely(string.toCharArray().asIterable())
    fun remove(string: String) = super.remove(string.toCharArray().asIterable())
    operator fun get(string: String) = super.get(string.toCharArray().asIterable())
    fun collect() = asSequence().map { String(it.toCharArray()) }.toSet()
}

open class IdentityTrie<T> : Trie<T, T>({ it })

/**
 * Implementation of a trie for any iterable values.
 */
open class Trie<T, K>(
    private val keyExtractor: (T) -> K
) : Iterable<List<T>> {

    private val roots = HashMap<K, NodeImpl<T, K>>()
    private val implementations = HashMap<Node<T>, NodeImpl<T, K>>()

    /**
     * Adds value into a trie.
     *
     * If value already exists then do nothing except increasing internal counter of added values.
     * The counter can be returned by [Node.count].
     *
     * @return corresponding [Node] of the last element in the `values`
     */
    fun add(values: Iterable<T>): Node<T> {
        val root = try { values.first() } catch (e: NoSuchElementException) { error("Empty list are not allowed") }
        var key = keyExtractor(root)
        var node = roots.computeIfAbsent(key) { NodeImpl(root, null) }
        values.asSequence().drop(1).forEach { value ->
            key = keyExtractor(value)
            node = node.children.computeIfAbsent(key) { NodeImpl(value, node) }
        }
        node.count++
        implementations[node] = node
        return node
    }

    /**
     * Decreases node counter value or removes the value completely if `counter == 1`.
     *
     * Use [removeCompletely] to remove the value from the trie regardless of counter value.
     *
     * @return removed node if value exists.
     */
    fun remove(values: Iterable<T>): Node<T>? {
        val node = findImpl(values) ?: return null
        return when {
            node.count == 1 -> removeCompletely(values)
            node.count > 1 -> node.apply { count-- }
            else -> throw IllegalStateException("count should be 1 or greater")
        }
    }

    /**
     * Removes value from a trie.
     *
     * The value is removed completely from the trie. Thus, the next code is true:
     *
     * ```
     * trie.remove(someValue)
     * trie.get(someValue) == null
     * ```
     *
     * Use [remove] to decrease counter value instead of removal.
     *
     * @return removed node if value exists
     */
    fun removeCompletely(values: Iterable<T>): Node<T>? {
        val node = findImpl(values) ?: return null
        if (node.count > 0 && node.children.isEmpty()) {
            var n: NodeImpl<T, K>? = node
            while (n != null) {
                val key = keyExtractor(n.data)
                n = n.parent
                if (n == null) {
                    val removed = roots.remove(key)
                    check(removed != null)
                } else {
                    val removed = n.children.remove(key)
                    check(removed != null)
                    if (n.count != 0) {
                        break
                    }
                }
            }
        }
        return if (node.count > 0) {
            node.count = 0
            implementations.remove(node)
            node
        } else {
            null
        }
    }

    operator fun get(values: Iterable<T>): Node<T>? {
        return findImpl(values)
    }

    operator fun get(node: Node<T>): List<T>? {
        return implementations[node]?.let(this::buildValue)
    }

    private fun findImpl(values: Iterable<T>): NodeImpl<T, K>? {
        val root = try { values.first() } catch (e: NoSuchElementException) { return null }
        var key = keyExtractor(root)
        var node = roots[key] ?: return null
        values.asSequence().drop(1).forEach { value ->
            key = keyExtractor(value)
            node = node.children[key] ?: return null
        }
        return node.takeIf { it.count > 0 }
    }

    override fun iterator(): Iterator<List<T>> {
        return iterator {
            roots.values.forEach { node ->
                traverseImpl(node)
            }
        }
    }

    private suspend fun SequenceScope<List<T>>.traverseImpl(node: NodeImpl<T, K>) {
        val stack = ArrayDeque<NodeImpl<T, K>>()
        stack.addLast(node)
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            if (n.count > 0) {
                yield(buildValue(n))
            }
            n.children.values.forEach(stack::addLast)
        }
    }

    private fun buildValue(node: NodeImpl<T, K>): List<T> {
        return generateSequence(node) { it.parent }.map { it.data }.toList().asReversed()
    }

    interface Node<T> {
        fun setTraceLen(len: Int)
        fun getTraceLen(): Int

        val data: T
        val count: Int
    }

    /**
     * Trie node
     *
     * @param data data to be stored
     * @param parent reference to the previous element of the value
     * @param count number of value insertions
     * @param children list of children mapped by their key
     */
    private class NodeImpl<T, K>(
        override val data: T,
        val parent: NodeImpl<T, K>?,
        override var count: Int = 0,
        val children: MutableMap<K, NodeImpl<T, K>> = HashMap(),
        private var traceLen: Int = -1
    ) : Node<T> {
        override fun setTraceLen(len: Int) {
            traceLen = len
        }

        override fun getTraceLen(): Int {
            return traceLen
        }

        override fun toString(): String {
            return "$data"
        }
    }

    private object EmptyNode : Node<Any> {
        override fun setTraceLen(len: Int) {}
        override fun getTraceLen(): Int {
            return -1
        }

        override val data: Any
            get() = error("empty node has no data")
        override val count: Int
            get() = 0

        override fun toString(): String {
            return "EMPTY"
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> emptyNode() = EmptyNode as Node<T>
    }
}