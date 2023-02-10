package org.utbot.python.typing

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

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
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val typesAdapter: JsonAdapter<List<PreprocessedValueFromJSON>> = moshi.adapter(Types.newParameterizedType(List::class.java, PreprocessedValueFromJSON::class.java))
        preprocessedTypes = typesAdapter.fromJson(typesAsString) ?: emptyList()
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

