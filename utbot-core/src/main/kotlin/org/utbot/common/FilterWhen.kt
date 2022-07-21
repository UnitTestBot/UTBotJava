package org.utbot.common

/**
 * If [condition] is true, returns a list containing only elements matching [predicate].
 * Otherwise, returns list with all elements of collection
 */
inline fun <T> Iterable<T>.filterWhen(condition: Boolean, predicate: (T) -> Boolean): List<T> =
    if (condition)
        this.filter(predicate)
    else
        this.toList()

/**
 * If [condition] is true, returns a sequence containing only elements matching [predicate].
 * Otherwise, leaves sequence unchanged
 */
fun <T> Sequence<T>.filterWhen(condition: Boolean, predicate: (T) -> Boolean): Sequence<T> =
    if (condition)
        this.filter(predicate)
    else
        this