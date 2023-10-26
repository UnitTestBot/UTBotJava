package org.utbot.python.framework.external

import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.api.python.util.moduleOfType

data class PythonObjectName(
    val moduleName: String,
    val name: String,
) {
    constructor(fullName: String) : this(
        moduleOfType(fullName) ?: pythonBuiltinsModuleName,
        fullName.removePrefix(moduleOfType(fullName) ?: pythonBuiltinsModuleName).removePrefix(".")
    )
    val fullName = "$moduleName.$name"
}
