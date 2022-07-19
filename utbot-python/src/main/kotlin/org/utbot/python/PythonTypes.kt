package org.utbot.python.types

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.ClassId

data class PythonType(
    val name: String,
    val instances: List<String>
)

object PythonTypesStorage {
    val builtinTypes: List<PythonType>
    init {
        val typesAsString = PythonTypesStorage::class.java.getResource("/builtin_types.json")?.readText(Charsets.UTF_8)
            ?: error("Didn't find builtin_types.json")
        builtinTypes =  Klaxon().parseArray(typesAsString) ?: emptyList()
    }

    val typeNameMap: Map<String, PythonType> by lazy {
        val result = mutableMapOf<String, PythonType>()
        builtinTypes.forEach { type ->
            result[type.name] = type
        }
        result
    }
}