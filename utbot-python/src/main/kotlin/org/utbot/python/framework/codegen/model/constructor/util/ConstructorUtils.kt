package org.utbot.python.framework.codegen.model.constructor.util

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.codegen.model.PythonUserImport
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall

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

internal fun PythonClassId.dropBuiltins(): PythonClassId {
    return if (this.rootModuleName == pythonBuiltinsModuleName) {
        val moduleParts = this.moduleName.split(".", limit = 2)
        if (moduleParts.size > 1) {
            PythonClassId(moduleParts[1], this.simpleName)
        } else {
            PythonClassId(this.name.split(".", limit = 2).last())
        }
    } else
        this
}

internal fun String.dropBuiltins(): String {
    val builtinsPrefix = "$pythonBuiltinsModuleName."
    return if (this.startsWith(builtinsPrefix))
        this.drop(builtinsPrefix.length)
    else
        this
}

