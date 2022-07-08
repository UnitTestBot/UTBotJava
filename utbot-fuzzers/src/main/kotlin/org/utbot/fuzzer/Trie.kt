package org.utbot.fuzzer

fun <T> trieOf(vararg values: Iterable<T>): Trie<T, T> = IdentityTrie<T>().apply {
    values.forEach(this::add)
}

fun stringTrieOf(vararg values: String): StringTrie = StringTrie().apply {
    values.forEach(this::add)
}

class StringTrie : IdentityTrie<Char>() {
    fun add(string: String) = super.add(string.toCharArray().asIterable())
    fun remove(string: String) = super.remove(string.toCharArray().asIterable())
    operator fun get(string: String) = super.get(string.toCharArray().asIterable())
    fun collect() = asSequence().map { String(it.toCharArray()) }.toSet()
}

open class IdentityTrie<T> : Trie<T, T>({it})

open class Trie<T, K>(
    private val keyExtractor: (T) -> K
) : Iterable<List<T>> {

    private val roots = HashMap<K, NodeImpl<T, K>>()
    private val implementations = HashMap<Node<T>, NodeImpl<T, K>>()

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

    fun remove(values: Iterable<T>): Node<T>? {
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

    interface Node<T>{
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
    ) : Node<T>
}