package org.utbot.common

/**
 * https://en.wikipedia.org/wiki/Multiset
 * Set where each element has integer multiplicity
 */
open class Multiset<T>(internal open val internal: Map<T, Int>) : Set<T> by internal.keys {
    /**
     * Returns element's multiplicity
     */
    operator fun get(k: T): Int = internal[k] ?: 0

    val sumOfMultiplicities: Int get() = internal.values.sum()
}

/**
 * Returns empty [MutableMultiset]
 */
fun <T> mutableMultisetOf() = MutableMultiset(mutableMapOf<T, Int>())

/**
 * https://en.wikipedia.org/wiki/Multiset
 * Mutable set where each element has integer multiplicity. You can add/remove elements.
 */
class MutableMultiset<T>(override val internal: MutableMap<T, Int>) : Multiset<T>(internal) {
    private fun adjust(element: T, count: Int): Boolean {
        val oldMultiplicity = internal[element]?.also {
            assert(it > 0) { "old multiplicity of `$element` must be > 0, but it's $it" }
        } ?: 0
        val newMultiplicity = oldMultiplicity + count

        require(newMultiplicity >= 0) {
            "new multiplicity (old + count) for `$element` must be >= 0, " +
                    "but it's $newMultiplicity, old multiplicity=$oldMultiplicity, count = $count"
        }

        if (oldMultiplicity > 0 && newMultiplicity == 0)
            remove(element)

        if (newMultiplicity > 0)
            internal[element] = newMultiplicity

        return ((oldMultiplicity == 0) xor (newMultiplicity == 0))
    }

    /**
     * Increases multiplicity of [element] by 1.
     * @return true if element's multiplicity was 0
     */
    fun add(element: T): Boolean = adjust(element, 1)


    @Suppress("unused")
    fun addAll(other: MutableMultiset<T>) {
        for (k in other)
            adjust(k, other[k])
    }

    fun clear() = internal.clear()

    override fun iterator(): MutableIterator<T> = internal.keys.iterator()

    /**
     * Try to decrease multiplicity of [element] by 1.
     * @return true if element's multiplicity became 0
     */
    fun remove(element: T): Boolean {
        if (!internal.containsKey(element))
            return false

        return adjust(element, -1)
    }

}