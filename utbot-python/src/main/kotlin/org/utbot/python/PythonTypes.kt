package org.utbot.python

import com.beust.klaxon.Klaxon

data class PythonType(
    val name: String,
    val instances: List<String>,
    val useAsReturn: Boolean
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