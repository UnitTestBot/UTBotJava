package framework.codegen.model.constructor.util

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet

internal operator fun <T> PersistentList<T>.plus(element: T): PersistentList<T> =
    this.add(element)

internal operator fun <T> PersistentList<T>.plus(other: PersistentList<T>): PersistentList<T> =
    this.addAll(other)

internal operator fun <T> PersistentSet<T>.plus(element: T): PersistentSet<T> =
    this.add(element)

internal operator fun <T> PersistentSet<T>.plus(other: PersistentSet<T>): PersistentSet<T> =
    this.addAll(other)
