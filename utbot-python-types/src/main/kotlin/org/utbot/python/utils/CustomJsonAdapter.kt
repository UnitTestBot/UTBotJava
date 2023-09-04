package org.utbot.python.utils

import com.squareup.moshi.*
import com.squareup.moshi.JsonAdapter.Factory
import java.lang.reflect.Type

class CustomPolymorphicJsonAdapterFactory(
    private val baseType: Type,
    contentLabel: String,
    keyLabel: String,
    private val elementAdapters: Map<String, Type>
): Factory {
    private val contentOption = JsonReader.Options.of(contentLabel)
    private val keyLabelOption = JsonReader.Options.of(keyLabel)
    private val labels = elementAdapters.keys.toList()
    private val labelOptions = JsonReader.Options.of(*labels.toTypedArray<String>())

    class CustomPolymorphicJsonAdapter(
        private val contentOption: JsonReader.Options,
        private val keyLabelOption: JsonReader.Options,
        private val adapters: List<JsonAdapter<Any>>,
        private val labelOptions: JsonReader.Options
    ): JsonAdapter<Any>() {
        override fun fromJson(reader: JsonReader): Any? {
            reader.beginObject()

            val index = reader.selectName(keyLabelOption)
            if (index == -1)
                return null

            val labelIndex = reader.selectString(labelOptions)
            if (labelIndex == -1)
                return null

            val contentIndex = reader.selectName(contentOption)
            if (contentIndex == -1)
                return null

            val result = adapters[labelIndex].fromJson(reader)
            reader.endObject()

            return result
        }

        override fun toJson(writer: JsonWriter, value: Any?) {
            error("Writing with this Json adapter is not supported")
        }
    }

    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != baseType || annotations.isNotEmpty()) {
            return null
        }
        val adapters: List<JsonAdapter<Any>> = labels.map { moshi.adapter(elementAdapters[it]!!) }
        return CustomPolymorphicJsonAdapter(contentOption, keyLabelOption, adapters, labelOptions)
    }
}