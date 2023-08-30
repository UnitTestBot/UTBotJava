package org.utbot.fuzzing.spring.valid

import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.providers.nullFuzzedValue
import org.utbot.fuzzing.spring.FuzzedTypeProperty

abstract class AbstractPrimitiveValidValueProvider<TProp : FuzzedTypeProperty<T>, T, V> :
    AbstractValidValueProvider<TProp, T>() {

    protected abstract val primitiveClass: Class<V>

    protected abstract fun defaultValidPrimitiveValue(validationData: T): V
    protected abstract fun makeValid(originalValue: V, validationData: T): V
    protected abstract fun isNullValid(): Boolean

    override fun acceptsType(type: FuzzedType): Boolean =
        primitiveClass.id == type.classId

    final override fun makeValid(originalValue: FuzzedValue, validationData: T): FuzzedValue =
        when (val model = originalValue.model) {
            is UtNullModel -> if (isNullValid()) originalValue else defaultValidValue(validationData)
            is UtPrimitiveModel -> {
                if (primitiveClass.isInstance(model.value))
                    primitiveFuzzedValue(makeValid(primitiveClass.cast(model.value), validationData))
                else
                    originalValue
            }
            else -> originalValue
        }

    final override fun defaultValidValue(validationData: T): FuzzedValue =
        primitiveFuzzedValue(defaultValidPrimitiveValue(validationData))

    private fun primitiveFuzzedValue(value: V) = value?.let {
        UtPrimitiveModel(value).fuzzed {
            summary = "%var% = ${
                when (value) {
                    is String -> "\"$value\""
                    is Char -> "\'$value\'"
                    else -> value.toString()
                }
            }"
        }
    } ?: nullFuzzedValue(primitiveClass.id)

}