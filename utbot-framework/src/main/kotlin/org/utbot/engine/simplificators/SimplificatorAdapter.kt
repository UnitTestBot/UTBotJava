package org.utbot.engine.simplificators

import java.util.IdentityHashMap


interface SimplificatorAdapter<T> {
    fun simplify(expression: T): T
}

abstract class CachingSimplificatorAdapter<T> : SimplificatorAdapter<T> {
    private val cache: IdentityHashMap<T, T> = IdentityHashMap()

    final override fun simplify(expression: T): T =
        cache.getOrPut(expression) { simplifyImpl(expression) }

    protected abstract fun simplifyImpl(expression: T): T
}

