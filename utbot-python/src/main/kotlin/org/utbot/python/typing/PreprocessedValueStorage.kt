package org.utbot.python.typing

import com.beust.klaxon.Klaxon

data class PreprocessedValueFromJSON(
    val name: String,
    val instances: List<String>
)

object TypesFromJSONStorage {
    private val preprocessedTypes: List<PreprocessedValueFromJSON>

    init {
        val typesAsString = TypesFromJSONStorage::class.java.getResource("/preprocessed_values.json")
            ?.readText(Charsets.UTF_8)
            ?: error("Didn't find preprocessed_values.json")
        preprocessedTypes = Klaxon().parseArray(typesAsString) ?: emptyList()
    }

    private val typeNameMap: Map<String, PreprocessedValueFromJSON> by lazy {
        val result = mutableMapOf<String, PreprocessedValueFromJSON>()
        preprocessedTypes.forEach { type ->
            result[type.name] = type
        }
        result
    }

    fun getTypesFromJsonStorage(): Map<String, PreprocessedValueFromJSON> {
        return typeNameMap
    }
}

