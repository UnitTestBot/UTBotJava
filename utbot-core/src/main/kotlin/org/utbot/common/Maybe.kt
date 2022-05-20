package org.utbot.common

/**
 * Analogue of java's [java.util.Optional]
 */
class Maybe<out T> private constructor(val hasValue: Boolean, val value:T? ) {
    constructor(v: T) : this(true, v)
    companion object {
        fun empty() = Maybe(false, null)
    }

    /**
     * Returns [value] if [hasValue]. Otherwise throws exception
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrThrow() : T = if (!hasValue) {
        error("Maybe hasn't value")
    } else {
        value as T
    }

    override fun toString(): String = if (hasValue) "Maybe($value)" else "<empty>"
}