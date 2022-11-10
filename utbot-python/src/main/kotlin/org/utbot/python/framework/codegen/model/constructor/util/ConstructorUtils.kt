package org.utbot.python.framework.codegen.model.constructor.util

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import org.utbot.framework.codegen.PythonUserImport
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonMethodId

internal fun CgContextOwner.importIfNeeded(method: PythonMethodId) {
    collectedImports += PythonUserImport(method.moduleName)
}

internal fun CgContextOwner.importIfNeeded(pyClass: PythonClassId) {
    collectedImports += PythonUserImport(pyClass.moduleName)
}

internal operator fun <T> PersistentList<T>.plus(element: T): PersistentList<T> =
    this.add(element)

internal operator fun <T> PersistentList<T>.plus(other: PersistentList<T>): PersistentList<T> =
    this.addAll(other)

internal operator fun <T> PersistentSet<T>.plus(element: T): PersistentSet<T> =
    this.add(element)

internal operator fun <T> PersistentSet<T>.plus(other: PersistentSet<T>): PersistentSet<T> =
    this.addAll(other)

